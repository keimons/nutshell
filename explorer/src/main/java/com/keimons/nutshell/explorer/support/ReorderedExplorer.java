package com.keimons.nutshell.explorer.support;

import com.keimons.nutshell.explorer.AbstractExplorerService;
import com.keimons.nutshell.explorer.RejectedTrackExecutionHandler;
import com.keimons.nutshell.explorer.TrackBarrier;
import com.keimons.nutshell.explorer.internal.BitsTrackEventBus;
import com.keimons.nutshell.explorer.utils.XUtils;
import jdk.internal.vm.annotation.Contended;

import java.lang.invoke.VarHandle;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

/**
 * 环轨执行器
 * <p>
 * 轨道缓冲区意在既不添加派发线程，又能处理交叉投递问题。{@code IO线程 -> 派发线程 -> work线程}的模式能够避免任务的交叉投递，但是增加了一次额外的派发。
 * 取消{@code 派发线程}，则有可能产生交叉投递问题。按照顺序投递（KeyC + KeyA也按照先投递KeyA，再投递KeyC），也可能产生的交叉投递问题如下：
 * <pre>
 * IO-Thread-1, commit task1: KeyA + KeyB
 * IO-Thread-2, commit task2: KeyB + KeyC
 * IO-Thread-3, commit task3: KeyA + KeyC
 * </pre>
 * KeyA，KeyB，KeyC分别投递到队列：QueueA，QueueB，QueueC。在某一个时刻，任务投递情况如下：
 * <ul>
 *     <li>IO-Thread-3，投递task3到QueueA</li>
 *     <li>IO-Thread-1，投递task1到QueueA</li>
 *     <li>IO-Thread-1，投递task1到QueueB</li>
 *     <li>IO-Thread-2，投递task2到QueueB</li>
 *     <li>IO-Thread-2，投递task2到QueueC</li>
 *     <li>IO-Thread-3，投递task3到QueueC</li>
 * </ul>
 * 此时，各个队列中的任务：
 * <pre>
 * QueueA --> task1, task3 --> Thread-1
 * QueueB --> task2, task1 --> Thread-2
 * QueueC --> task3, task2 --> Thread-3
 * </pre>
 * 此时任务出现交叉，产生死锁。在不添加派发线程的前提下，升级环形队列，增加一个维度，每个线程仅仅读取指定槽位的Key，如果当前位置为空，则表示没有任务，跳过执行。示意如下：
 * <pre>
 *           writeIndex
 *           |
 *           +---------------------------+    +---------------------+
 * QueueA -> | Key1 | Key1 |      | Key1 | -> | Thread-1, readIndex |
 *           |------+------+------+------|    |---------------------|
 * QueueB -> |      | Key2 | Key2 |      | -> | Thread-2, readIndex |
 *           |------+------+------+------|    |---------------------|
 * QueueC -> | Key3 |      | Key3 |      | -> | Thread-3, readIndex |
 *           +---------------------------+    +---------------------+
 *              |      |      |      |
 *            task3  task1  task2  task0
 * </pre>
 * 由IO线程生成任务信息（考虑对象池）并发布在环形Buffer总线上。环形buffer中发布的，不再是单个任务，而是包含Key组的任务，Key组中可能包含一个或多个Key。
 * 仅仅维护一个全局的{@code writeIndex}，每个线程维护自己的{@code readIndex}，只要{@code readIndex < writeIndex}
 * 则可以继续向下读取，如果当前位置为空，则表明此任务不是这个线程关注的任务，跳过执行，联合{@link TrackBarrier}使用。
 * <p>
 * 轨道缓冲区同时也是总线队列，所有任务都是发布在总线上。
 * <p>
 * 其它：
 * <ul>
 *     <li>任务发布，环形Buffer总线上发布由IO线程生成的任务。</li>
 *     <li>占用更多空间（n * ThreadCount），队列利用率下降。</li>
 *     <li>
 *         任务命中率降至{@code 1/ThreadCount}。TODO 参考链表实现，提升命中率
 *     </li>
 *     <li>充分利用cpu缓存行能力下降。</li>
 * </ul>
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
public class ReorderedExplorer extends AbstractExplorerService {

	public static final int DEFAULT_THREAD_CAPACITY = 1024;

	public static final String DEFAULT_NAME = "ReorderedExplorer";

	public static final VarHandle BB = XUtils.findVarHandle(ReorderedTrackWorker.class, "parked", boolean.class);

	/**
	 * 事件总线
	 * <p>
	 * 所有任务都发布在事件总线上，如果事件总线不能发布任务，任务发布失败，则队列已满。
	 */
	private final BitsTrackEventBus<Node> eventBus;

	/**
	 * 任务执行器
	 */
	private final ReorderedTrackWorker[] executors;

	public ReorderedExplorer(int nThreads) {
		this(DEFAULT_NAME, nThreads, nThreads * DEFAULT_THREAD_CAPACITY, DefaultRejectedHandler, Executors.defaultThreadFactory());
	}

	public ReorderedExplorer(String name, int nThreads, int capacity, RejectedTrackExecutionHandler rejectedHandler, ThreadFactory threadFactory) {
		super(name, nThreads, rejectedHandler, threadFactory);
		eventBus = new BitsTrackEventBus<>(capacity, nThreads);
		executors = new ReorderedTrackWorker[nThreads];
		for (int i = 0; i < nThreads; i++) {
			ReorderedTrackWorker executor = new ReorderedTrackWorker(i, threadFactory);
			executor.thread.start();
			executors[i] = executor;
		}
	}

	/**
	 * 唤醒线程
	 *
	 * @param track 轨道
	 */
	private void weakUp(int track) {
		ReorderedTrackWorker worker = executors[track];
		if (worker.parked) {
			worker.parked = false;
			LockSupport.unpark(worker.thread);
		}
	}

	@Override
	public void execute(Runnable task, Object fence) {
		Node node = new Node1(task, fence);
		eventBus.publishEvent(node);
		node.weakUpAll();
	}

	public void execute(Runnable task, Object fence0, Object fence1) {
		Node node = new Node2(task, fence0, fence1);
		eventBus.publishEvent(node);
		node.weakUpAll();
	}

	public void execute(Runnable task, Object fence0, Object fence1, Object fence2) {
		AbstractNode node = new Node3(task, fence0, fence1, fence2);
		eventBus.publishEvent(node);
		node.weakUpAll();
	}

	public void execute(Runnable task, Object... fences) {
		AbstractNode node = new NodeX(task, fences);
		eventBus.publishEvent(node);
		node.weakUpAll();
	}

	@Override
	public Future<?> submit(Runnable task, Object fence) {
		RunnableFuture<Void> future = new FutureTask<>(task, null);
		execute(future, fence);
		return future;
	}

	public Future<?> submit(Runnable task, Object fence1, Object fence2) {
		RunnableFuture<Void> future = new FutureTask<>(task, null);
		execute(future, fence1, fence2);
		return future;
	}

	@Override
	public <T> Future<T> submit(Callable<T> task, TrackBarrier barrier) {
		FutureTask<T> future = new FutureTask<>(task);
		execute(future, barrier);
		return future;
	}

	@Override
	public boolean isShutdown() {
		return !running;
	}

	@Override
	public void shutdown() {
		running = false;
		for (ReorderedTrackWorker executor : executors) {
			executor.shutdown();
		}
	}

	/**
	 * 支持重排序的环轨执行器
	 * <p>
	 * 轨道缓冲区意在既不添加派发线程，又能处理交叉投递问题。{@code IO线程 -> 派发线程 -> work线程}的模式能够避免任务的交叉投递，但是增加了一次额外的派发。
	 * 取消{@code 派发线程}，则有可能产生交叉投递问题。按照顺序投递（KeyC + KeyA也按照先投递KeyA，再投递KeyC），也可能产生的交叉投递问题如下：
	 * <pre>
	 * IO-Thread-1, commit task1: KeyA + KeyB
	 * IO-Thread-2, commit task2: KeyB + KeyC
	 * IO-Thread-3, commit task3: KeyA + KeyC
	 * </pre>
	 * KeyA，KeyB，KeyC分别投递到队列：QueueA，QueueB，QueueC。在某一个时刻，任务投递情况如下：
	 * <ul>
	 *     <li>IO-Thread-3，投递task3到QueueA</li>
	 *     <li>IO-Thread-1，投递task1到QueueA</li>
	 *     <li>IO-Thread-1，投递task1到QueueB</li>
	 *     <li>IO-Thread-2，投递task2到QueueB</li>
	 *     <li>IO-Thread-2，投递task2到QueueC</li>
	 *     <li>IO-Thread-3，投递task3到QueueC</li>
	 * </ul>
	 * 此时，各个队列中的任务：
	 * <pre>
	 * QueueA --> task1, task3 --> Thread-1
	 * QueueB --> task2, task1 --> Thread-2
	 * QueueC --> task3, task2 --> Thread-3
	 * </pre>
	 * 此时任务出现交叉，产生死锁。在不添加派发线程的前提下，升级环形队列，增加一个维度，每个线程仅仅读取指定槽位的Key，如果当前位置为空，则表示没有任务，跳过执行。示意如下：
	 * <pre>
	 *           writeIndex
	 *           |
	 *           +---------------------------+    +---------------------+
	 * QueueA -> | Key1 | Key1 |      | Key1 | -> | Thread-1, readIndex |
	 *           |------+------+------+------|    |---------------------|
	 * QueueB -> |      | Key2 | Key2 |      | -> | Thread-2, readIndex |
	 *           |------+------+------+------|    |---------------------|
	 * QueueC -> | Key3 |      | Key3 |      | -> | Thread-3, readIndex |
	 *           +---------------------------+    +---------------------+
	 *              |      |      |      |
	 *            task3  task1  task2  task0
	 * </pre>
	 * 由IO线程生成任务信息（考虑对象池）并发布在环形Buffer总线上。环形buffer中发布的，不再是单个任务，而是包含Key组的任务，Key组中可能包含一个或多个Key。
	 * 仅仅维护一个全局的{@code writeIndex}，每个线程维护自己的{@code readIndex}，只要{@code readIndex < writeIndex}
	 * 则可以继续向下读取，如果当前位置为空，则表明此任务不是这个线程关注的任务，跳过执行，联合{@link TrackBarrier}使用。
	 * <p>
	 * 轨道缓冲区同时也是总线队列，所有任务都是发布在总线上。
	 * <p>
	 * 其它：
	 * <ul>
	 *     <li>任务发布，环形Buffer总线上发布由IO线程生成的任务。</li>
	 *     <li>占用更多空间（n * ThreadCount），队列利用率下降。</li>
	 *     <li>
	 *         任务命中率降至{@code 1/nThreads}。TODO 参考链表实现，提升命中率
	 *     </li>
	 *     <li>充分利用cpu缓存行能力下降。</li>
	 * </ul>
	 *
	 * @author houyn[monkey@keimons.com]
	 * @version 1.0
	 * @since 11
	 **/
	private class ReorderedTrackWorker implements Runnable {

		@Contended
		protected final Thread thread;

		protected final int track;

		private int interceptIndex;

		private int barrierIndex;

		@Contended
		private volatile long readerIndex;

		/**
		 * 拦截队列
		 * <p>
		 * 当前线程中被屏障拦截的事件，直接缓存在执行器本地，等待拦截器释放后，再执行缓存的消息。
		 */
		private Node[] intercepted = new Node[8];

		/**
		 * 执行屏障
		 * <p>
		 * 事件总线上的事件，即使是由当前线程执行，也并不一定可以立即处理。
		 */
		private Node[] barriers = new Node[8];

		@Contended
		volatile boolean parked;

		/**
		 * 执行器
		 * <p>
		 * 消息的真正执行者
		 */
		public ReorderedTrackWorker(int track, ThreadFactory threadFactory) {
			this.track = track;
			this.thread = threadFactory.newThread(this);
		}

		private void _add0(Node node) {
			if (interceptIndex >= intercepted.length) {
				Node[] tmp = new Node[interceptIndex << 1];
				System.arraycopy(intercepted, 0, tmp, 0, interceptIndex);
				intercepted = tmp;
			}
			intercepted[interceptIndex++] = node;
		}

		private void _add1(Node node) {
			if (barrierIndex >= barriers.length) {
				Node[] tmp = new Node[barrierIndex << 1];
				System.arraycopy(barriers, 0, tmp, 0, barrierIndex);
				barriers = tmp;
			}
			barriers[barrierIndex++] = node;
		}

		private void _remove0(int index) {
			for (int i = index, limit = interceptIndex - 1; i < limit; i++) {
				intercepted[i] = intercepted[i + 1];
			}
			intercepted[--interceptIndex] = null;
		}

		private void _remove1(int index) {
			for (int i = index, limit = barrierIndex - 1; i < limit; i++) {
				barriers[i] = barriers[i + 1];
			}
			barriers[--barrierIndex] = null;
		}

		private boolean isReorder(Node node) {
			// 判断是否可以越过所有屏障执行任务
			for (int i = 0; i < barrierIndex; i++) {
				if (!node.isReorder(barriers[i])) {
					return false;
				}
			}
			return true;
		}

		private Node next() {
			while (true) {
				for (int i = 0; i < barrierIndex; i++) {
					Node node = barriers[i];
					if (!node.isIntercepted()) {
//						Debug.info("Work-" + track + " 移除屏障：" + node.getTask());
						_remove1(i);
					}
				}
				for (int i = 0; i < interceptIndex; i++) {
					Node node = intercepted[i];
					if (isReorder(node)) {
						// 这个任务已经可以执行了，所以，直接移除
						_remove0(i);
						if (node.tryIntercept()) {
//							Debug.info("Work-" + track + " 恢复屏障：" + node.getTask());
							_add1(node);
							node.weakUpAll();
						} else {
//							Debug.info("Work-" + track + " 恢复任务：" + node.getTask());
							if (!node.isSingle()) {
								eventBus.finishEvent(node.getIndex());
							}
							return node;
						}
					}
				}
				final long readerIndex = this.readerIndex;
				if (readerIndex < eventBus.writerIndex()) {
					Node node = eventBus.getEvent(readerIndex);
					this.readerIndex = readerIndex + 1;
					if (node == null) {
						continue;
					}
					if (node.isTrack(1L << track)) {
						if (isReorder(node)) {
							if (node.tryIntercept()) {
								// only execute thread return event
//								Debug.info("Work-" + track + " 增加屏障：" + node.getTask());
								_add1(node);
							} else {
//								Debug.info("Work-" + track + " 执行任务：" + node.getTask());
								eventBus.finishEvent(readerIndex);
								return node;
							}
						} else {
//							Debug.info("Work-" + track + " 缓存任务：" + node.getTask());
							if (node.isSingle()) {
								eventBus.finishEvent(readerIndex);
							}
							node.setIndex(readerIndex);
							_add0(node);
						}
					}
				} else {
					return null;
				}
			}
		}

		/**
		 * 关闭线程
		 */
		public void shutdown() {

		}

		@Override
		public void run() {
			while (running) {
				try {
					while (running) {
						Node node = null;
						try {
							node = next();
							if (node == null) {
								parked = true;
								node = next();
								if (node == null) {
									LockSupport.park();
								}
							}
							if (node != null) {
								node.getTask().run();
							}
						} catch (Throwable e) {
							// ignore
							e.printStackTrace();
						} finally {
							if (node != null) {
								node.release(track);
							}
						}
					}
				} catch (Throwable e) {
					// ignore
					e.printStackTrace();
				}
			}
		}

		@Override
		public String toString() {
			return "ReorderTrackWorker-" + track;
		}
	}

	private static final VarHandle VV = XUtils.findVarHandle(AbstractNode.class, "forbids", int.class);

	public interface Node {

		void setIndex(long index);

		long getIndex();

		Runnable getTask();

		int getFenceCount();

		boolean tryIntercept();

		boolean isIntercepted();

		boolean isTrack(long bits);

		/**
		 * 判断节点是否该轨道上
		 *
		 * @param track 轨道
		 * @return 是否在当前轨道
		 */
		boolean isTrack(int track);

		boolean isSingle();

		void weakUpAll();

		boolean isReorder(Node other);

		/**
		 * 释放节点
		 *
		 * @param track 执行任务的轨道
		 */
		void release(int track);
	}

	public abstract static class AbstractNode implements Node {

		/**
		 * 任务位置
		 * <p>
		 * 共有{@link #nThreads}条轨道，当前任务所处的轨道位置（可能不止一个）。轨道数量不超过64，所以使用{@code bits}存储。
		 */
		protected long bits;

		protected final int nParams;

		protected final Runnable task;

		long index;

		@Contended
		protected volatile int forbids;

		protected volatile boolean intercepted = true;

		protected AbstractNode(Runnable task, int nParams) {
			this.nParams = nParams;
			this.task = task;
		}

		@Override
		public long getIndex() {
			return index;
		}

		@Override
		public void setIndex(long index) {
			this.index = index;
		}

		@Override
		public Runnable getTask() {
			return task;
		}

		@Override
		public int getFenceCount() {
			return nParams;
		}

		public boolean tryIntercept() {
			int v;
			do {
				v = forbids;
			} while (!VV.compareAndSet(this, v, v - 1));
			return v > 0;
		}

		public boolean isIntercepted() {
			return intercepted;
		}

		public boolean isTrack(long bits) {
			return (this.bits & bits) != 0;
		}

		/**
		 * 判断节点是否该轨道上
		 *
		 * @param track 轨道
		 * @return 是否在当前轨道
		 */
		public boolean isTrack(int track) {
			return (bits & 1L << track) != 0;
		}

		@Override
		public boolean isSingle() {
			return Long.bitCount(bits) <= 1;
		}

		public abstract void weakUpAll();

		public abstract boolean isReorder(Node other);

		/**
		 * 释放节点
		 *
		 * @param track 执行任务的轨道
		 */
		public abstract void release(int track);
	}

	public class Node1 implements Node {

		protected final Runnable task;

		private final Object fence;

		private final int track;

		long index;

		public Node1(Runnable task, Object fence) {
			this.task = task;
			this.track = fence.hashCode() % nThreads;
			this.fence = fence;
		}

		@Override
		public void setIndex(long index) {
			this.index = index;
		}

		@Override
		public long getIndex() {
			return index;
		}

		@Override
		public Runnable getTask() {
			return task;
		}

		@Override
		public int getFenceCount() {
			return 0;
		}

		@Override
		public boolean tryIntercept() {
			// must false
			return false;
		}

		@Override
		public boolean isIntercepted() {
			return false;
		}

		@Override
		public boolean isTrack(long bits) {
			return ((1L << track) & bits) != 0;
		}

		@Override
		public boolean isTrack(int track) {
			return false;
		}

		@Override
		public boolean isSingle() {
			return true;
		}

		@Override
		public void weakUpAll() {
			weakUp(track);
		}

		/**
		 * 判断另一个节点是否能越过当前节点执行
		 *
		 * @param other 后续节点
		 * @return {@code true}可以越过当前栅栏，{@code false}不能越过当前栅栏
		 */
		@Override
		public boolean isReorder(Node other) {
			switch (other.getFenceCount()) {
				case 1: {
					throw new IllegalStateException();
				}
				case 2: {
					Node2 node2 = (Node2) other;
					return !(node2.fence0.equals(fence) || node2.fence1.equals(fence));
				}
				case 3: {
					Node3 node3 = (Node3) other;
					return !(node3.fence0.equals(fence) || node3.fence1.equals(fence) || node3.fence2.equals(fence));
				}
				default: {
					NodeX nodeX = (NodeX) other;
					for (int i = 0, count = nodeX.nParams; i < count; i++) {
						Object v = nodeX.fences[i];
						if (v.equals(fence)) {
							return false;
						}
					}
					return true;
				}
			}
		}

		@Override
		public void release(int track) {
			// do nothing
		}
	}

	public class Node2 extends AbstractNode {

		private final int track1;

		private final Object fence0;

		private final int track2;

		private final Object fence1;

		public Node2(Runnable task, Object fence0, Object fence1) {
			super(task, 2);
			this.track1 = fence0.hashCode() % nThreads;
			this.track2 = fence1.hashCode() % nThreads;
			this.fence0 = fence0;
			this.fence1 = fence1;
			this.bits = (1L << this.track1) | (1L << (this.track2));
			this.forbids = track1 == track2 ? 0 : 1;
		}

		@Override
		public void weakUpAll() {
			weakUp(track1);
			weakUp(track2);
		}

		/**
		 * 判断另一个节点是否能越过当前节点执行
		 *
		 * @param other 后续节点
		 * @return {@code true}可以越过当前栅栏，{@code false}不能越过当前栅栏
		 */
		@Override
		public boolean isReorder(Node other) {
			switch (other.getFenceCount()) {
				case 1: {
					throw new IllegalStateException();
				}
				case 2: {
					Node2 node2 = (Node2) other;
					return !(node2.fence0.equals(fence0) || node2.fence0.equals(fence1) || node2.fence1.equals(fence0) || node2.fence1.equals(fence1));
				}
				case 3: {
					Node3 node3 = (Node3) other;
					return !(node3.fence0.equals(fence0) ||
							node3.fence0.equals(fence1) ||
							node3.fence1.equals(fence0) ||
							node3.fence1.equals(fence1) ||
							node3.fence2.equals(fence0) ||
							node3.fence2.equals(fence1));
				}
				default: {
					NodeX nodeX = (NodeX) other;
					for (int i = 0, count = nodeX.nParams; i < count; i++) {
						Object v = nodeX.fences[i];
						if (v.equals(fence0) || v.equals(fence1)) {
							return false;
						}
					}
					return true;
				}
			}
		}

		@Override
		public void release(int track) {
			intercepted = false;
			if (track != track1) {
				weakUp(track1);
			}
			if (track != track2) {
				weakUp(track2);
			}
		}
	}

	public class Node3 extends AbstractNode {

		private final int track1;

		private final Object fence0;

		private final int track2;

		private final Object fence1;

		private final int track3;

		private final Object fence2;

		public Node3(Runnable task, Object fence0, Object fence1, Object fence2) {
			super(task, 3);
			this.track1 = fence0.hashCode() % nThreads;
			this.track2 = fence1.hashCode() % nThreads;
			this.track3 = fence2.hashCode() % nThreads;
			this.fence0 = fence0;
			this.fence1 = fence1;
			this.fence2 = fence2;
			this.bits = (1L << this.track1) | (1L << (this.track2)) | (1L << (this.track3));
			if (this.track1 == this.track2 && this.track1 == this.track3) {
				this.forbids = 0;
			} else if (this.track1 == this.track2 || this.track1 == this.track3 || this.track2 == this.track3) {
				this.forbids = 1;
			} else {
				this.forbids = 2;
			}
		}

		@Override
		public void weakUpAll() {
			weakUp(track1);
			weakUp(track2);
			weakUp(track3);
		}

		/**
		 * 判断另一个节点是否能越过当前节点执行
		 *
		 * @param other 后续节点
		 * @return {@code true}可以越过当前栅栏，{@code false}不能越过当前栅栏
		 */
		@Override
		public boolean isReorder(Node other) {
			switch (other.getFenceCount()) {
				case 1: {
					throw new IllegalStateException();
				}
				case 2: {
					Node2 node2 = (Node2) other;
					return !(node2.fence0.equals(fence0) || node2.fence0.equals(fence1) || node2.fence0.equals(fence2) ||
							node2.fence1.equals(fence0) || node2.fence1.equals(fence1) || node2.fence1.equals(fence2));
				}
				case 3: {
					Node3 node3 = (Node3) other;
					return !(node3.fence0.equals(fence0) ||
							node3.fence0.equals(fence1) ||
							node3.fence0.equals(fence2) ||
							node3.fence1.equals(fence0) ||
							node3.fence1.equals(fence1) ||
							node3.fence1.equals(fence2) ||
							node3.fence2.equals(fence0) ||
							node3.fence2.equals(fence1) ||
							node3.fence2.equals(fence2));
				}
				default: {
					NodeX nodeX = (NodeX) other;
					for (int i = 0, count = nodeX.nParams; i < count; i++) {
						Object v = nodeX.fences[i];
						if (v.equals(fence0) || v.equals(fence1) || v.equals(fence2)) {
							return false;
						}
					}
					return true;
				}
			}
		}

		@Override
		public void release(int track) {
			intercepted = false;
			if (track != track1) {
				weakUp(track1);
			}
			if (track != track2) {
				weakUp(track2);
			}
			if (track != track3) {
				weakUp(track3);
			}
		}
	}

	public class NodeX extends AbstractNode {

		private final Object[] fences;

		public NodeX(Runnable task, Object... fences) {
			super(task, fences.length);
			this.fences = fences;
			for (int i = 0; i < nParams; i++) {
				this.bits |= (1L << fences[i].hashCode() % nThreads);
			}
			this.forbids = Long.bitCount(bits) - 1;
		}

		@Override
		public void weakUpAll() {
			for (int i = 0; i < nThreads; i++) {
				if (isTrack(i)) {
					weakUp(i);
				}
			}
		}

		/**
		 * 判断另一个节点是否能越过当前节点执行
		 *
		 * @param other 后续节点
		 * @return {@code true}可以越过当前栅栏，{@code false}不能越过当前栅栏
		 */
		@Override
		public boolean isReorder(Node other) {
			switch (other.getFenceCount()) {
				case 1: {
					throw new IllegalStateException();
				}
				case 2: {
					Node2 node = (Node2) other;
					for (int i = 0; i < nParams; i++) {
						Object fence = fences[i];
						if (fence.equals(node.fence0) || fence.equals(node.fence1)) {
							return false;
						}
					}
					return true;
				}
				case 3: {
					Node3 node = (Node3) other;
					for (int i = 0; i < nParams; i++) {
						Object fence = fences[i];
						if (fence.equals(node.fence0) || fence.equals(node.fence1) || fence.equals(node.fence2)) {
							return false;
						}
					}
					return true;
				}
				default: {
					NodeX node = (NodeX) other;
					for (int i = 0, count = node.nParams; i < count; i++) {
						Object v = node.fences[i];
						for (int j = 0; j < nParams; j++) {
							if (v.equals(fences[j])) {
								return false;
							}
						}
					}
					return true;
				}
			}
		}

		@Override
		public void release(int track) {
			intercepted = false;
			for (int i = 0; i < nThreads; i++) {
				if (isTrack(i)) {
					weakUp(i);
				}
			}
		}
	}
}
