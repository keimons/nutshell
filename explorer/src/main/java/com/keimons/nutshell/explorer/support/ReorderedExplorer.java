package com.keimons.nutshell.explorer.support;

import com.keimons.nutshell.explorer.*;
import com.keimons.nutshell.explorer.internal.DefaultEventBus;
import com.keimons.nutshell.explorer.utils.XUtils;
import jdk.internal.vm.annotation.Contended;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.VarHandle;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Predicate;

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

	/**
	 * 默认线程队列长度
	 * <p>
	 * 为每个线程分配2048的队列。例如，拥有4个线程的线程池，则队列总长度为{@code 4 * 2048 = 8192}。
	 */
	public static final int DEFAULT_THREAD_CAPACITY = 2048;

	public static final AtomicInteger EXPLORER_WATCHER_INDEX = new AtomicInteger();

	/**
	 * 默认线程池名称
	 */
	public static final String DEFAULT_NAME = "ReorderedExplorer";

	/**
	 * 事件总线
	 * <p>
	 * 所有任务都发布在事件总线上，如果事件总线不能发布任务，任务发布失败，则队列已满。
	 */
	private final DefaultEventBus<Node> eventBus;

	/**
	 * 任务执行器
	 */
	private final Walker[] walkers;

	/**
	 * 守望线程
	 */
	private final Watcher watcher;

	public ReorderedExplorer(int nThreads) {
		this(DEFAULT_NAME, nThreads, nThreads * DEFAULT_THREAD_CAPACITY, DefaultRejectedHandler, Explorers.defaultThreadFactory());
	}

	public ReorderedExplorer(String name, int nThreads, int capacity, RejectedTrackExecutionHandler rejectedHandler, ThreadFactory threadFactory) {
		super(name, nThreads, rejectedHandler, threadFactory);
		eventBus = new DefaultEventBus<>(capacity);
		walkers = new Walker[nThreads];
		for (int track = 0; track < nThreads; track++) {
			Walker walker = new Walker(track);
			walker.thread.start();
			walkers[track] = walker;
		}
		watcher = new Watcher();
		Thread thread = new Thread(watcher, "ExplorerWatcher-" + EXPLORER_WATCHER_INDEX.getAndIncrement());
		thread.setDaemon(true);
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
	}

	/**
	 * 唤醒线程
	 *
	 * @param track 轨道
	 */
	private void weakUp(int track) {
		Walker worker = walkers[track];
		worker.increment();
		if (worker.blocked) {
			worker.blocked = false;
			LockSupport.unpark(worker.thread);
		}
	}

	@Override
	public void execute(Runnable task, Object fence) {
		if (task == null || fence == null) {
			throw new NullPointerException();
		}
		if (state > RUNNING) {
			rejectedHandler.rejectedExecution(this, task, fence);
		} else {
			Node node = new Node1(task, fence);
			if (eventBus.publishEvent(node)) {
				node.weakUp();
			} else {
				rejectedHandler.rejectedExecution(this, task, fence);
			}
		}
	}

	public void execute(Runnable task, Object fence0, Object fence1) {
		if (task == null || fence0 == null || fence1 == null) {
			throw new NullPointerException();
		}
		if (state > RUNNING) {
			rejectedHandler.rejectedExecution(this, task, fence0, fence1);
		} else {
			Node node = new Node2(task, fence0, fence1);
			if (eventBus.publishEvent(node)) {
				node.weakUp();
			} else {
				rejectedHandler.rejectedExecution(this, task, fence0, fence1);
			}
		}
	}

	public void execute(Runnable task, Object fence0, Object fence1, Object fence2) {
		if (task == null || fence0 == null || fence1 == null || fence2 == null) {
			throw new NullPointerException();
		}
		if (state > RUNNING) {
			rejectedHandler.rejectedExecution(this, task, fence0, fence1, fence2);
		} else {
			Node node = new Node3(task, fence0, fence1, fence2);
			if (eventBus.publishEvent(node)) {
				node.weakUp();
			} else {
				rejectedHandler.rejectedExecution(this, task, fence0, fence1, fence2);
			}
		}
	}

	public void execute(Runnable task, Object... fences) {
		if (task == null) {
			throw new NullPointerException();
		}
		for (int i = 0, count = fences.length; i < count; i++) {
			if (fences[i] == null) {
				throw new NullPointerException();
			}
		}
		if (state > RUNNING) {
			rejectedHandler.rejectedExecution(this, task, fences);
		} else {
			Node node = new NodeX(task, fences);
			if (eventBus.publishEvent(node)) {
				node.weakUp();
			} else {
				rejectedHandler.rejectedExecution(this, task, fences);
			}
		}
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

	public Future<?> submit(Runnable task, Object fence1, Object fence2, Object fence3) {
		RunnableFuture<Void> future = new FutureTask<>(task, null);
		execute(future, fence1, fence2, fence3);
		return future;
	}

	public Future<?> submit(Runnable task, Object... fences) {
		switch (fences.length) {
			case 0: {
				throw new RuntimeException();
			}
			case 1: {
				return submit(task, fences[0]);
			}
			case 2: {
				return submit(task, fences[0], fences[1]);
			}
			case 3: {
				return submit(task, fences[0], fences[1], fences[2]);
			}
			default: {
				RunnableFuture<Void> future = new FutureTask<>(task, null);
				execute(future, fences);
				return future;
			}
		}
	}

	@Override
	public <T> Future<T> submit(Callable<T> task, Object fence) {
		FutureTask<T> future = new FutureTask<>(task);
		execute(future, fence);
		return future;
	}

	@Override
	public boolean isShutdown() {
		return !running;
	}

	@Override
	public Future<?> close() {
		state = CLOSE;
		eventBus.shutdown();
		for (Walker worker : walkers) {
			worker.thread.interrupt();
		}
		return new CloseFuture((explorer) -> {
			int sum = 0;
			for (Walker walker : walkers) {
				sum += walker.completedTasks;
			}
			return sum >= eventBus.writerIndex();
		});
	}

	/**
	 * // 我们再三考虑决定，移除对于shutdown的支持。
	 * // 因为对于支持重排序的线程池来说，所有任务已经结束。
	 *
	 * @throws UnsupportedOperationException 不支持的{@code shutdown()}调用
	 */
	@Override
	public void shutdown() {
		throw new UnsupportedOperationException();
	}

	private static final VarHandle L = XUtils.findVarHandle(Walker.class, "stamp", int.class);

	/**
	 * <pre>
	 *     +------+
	 *     | read |
	 *     +------+
	 *        ||
	 *    +---------+  N   +-----+
	 *    | isTrack | ---> | END |
	 *    +---------+      +-----+
	 *       Y |
	 *    +---------+  Y   +-------+
	 *    | reorder | ---> | cache |
	 *    +---------+      +-------+
	 *       N |
	 *    +-------+
	 *    | cache |
	 *    +-------+
	 * </pre>
	 * <p>
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
	 *     <li>任务命中率降至{@code 1/nThreads}。</li>
	 *     <li>充分利用cpu缓存行能力下降。</li>
	 * </ul>
	 *
	 * @author houyn[monkey@keimons.com]
	 * @version 1.0
	 * @since 11
	 **/
	private class Walker implements Runnable {

		/**
		 * 线程运行轨道
		 * <p>
		 * 线程会有自己的运行轨道，并且，只会处理落在这个轨道上的节点。节点可以由多个线程处理，
		 * 如果节点由当前线程处理，才会进行后续操作。
		 */
		protected final int track;

		/**
		 *
		 */
		private int cacheIndex;

		private int barrierIndex;

		private long readerIndex;

		/**
		 * 已完成的任务数量
		 */
		long completedTasks;

		/**
		 * 拦截队列
		 * <p>
		 * 当前线程中被屏障拦截的事件，直接缓存在执行器本地，等待拦截器释放后，再执行缓存的消息。
		 */
		private Node[] caches = new Node[8];

		/**
		 * 执行屏障
		 * <p>
		 * 事件总线上的事件，即使是由当前线程执行，也并不一定可以立即处理。
		 */
		private Node[] barriers = new Node[8];

		@Contended
		protected Thread thread;

		@Contended
		volatile int stamp;

		@Contended
		volatile boolean blocked;

		/**
		 * 自增
		 */
		public void increment() {
			int v;
			do {
				v = stamp;
			} while (!L.compareAndSet(this, v, v + 1));
		}

		/**
		 * 执行器构造方法
		 * <p>
		 * {@link Node}的真正处理者。
		 *
		 * @param track 线程运行轨道
		 */
		public Walker(int track) {
			this.track = track;
			this.thread = threadFactory.newThread(this);
		}

		private void _addCache(Node node) {
			if (cacheIndex >= caches.length) {
				Node[] tmp = new Node[cacheIndex << 1];
				System.arraycopy(caches, 0, tmp, 0, cacheIndex);
				caches = tmp;
			}
			caches[cacheIndex++] = node;
		}

		private void _addBarrier(Node node) {
			if (barrierIndex >= barriers.length) {
				Node[] tmp = new Node[barrierIndex << 1];
				System.arraycopy(barriers, 0, tmp, 0, barrierIndex);
				barriers = tmp;
			}
			barriers[barrierIndex++] = node;
		}

		private void _removeCache(int index) {
			for (int i = index, limit = cacheIndex - 1; i < limit; i++) {
				caches[i] = caches[i + 1];
			}
			caches[--cacheIndex] = null;
		}

		private void _removeBarrier(int index) {
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
			Node node;
			for (; ; ) {
				// 状态检测，如果线程池已停止
				if (state >= CLOSE && eventBus.testWriterIndex(readerIndex)) {
					return null;
				}
				for (int i = 0; i < barrierIndex; i++) {
					node = barriers[i];
					if (!node.isIntercepted()) {
//						Debug.info("Work-" + track + " 移除屏障：" + node.getTask());
						_removeBarrier(i);
					}
				}
				for (int i = 0; i < cacheIndex; i++) {
					node = caches[i];
					if (isReorder(node)) {
						// 这个任务已经可以执行了，所以，直接移除
						_removeCache(i);
						if (node.tryIntercept()) {
//							Debug.info("Work-" + track + " 恢复屏障：" + node.getTask());
							_addBarrier(node);
							node.weakUp();
						} else {
//							Debug.info("Work-" + track + " 恢复任务：" + node.getTask());
							if (!node.isAloneTrack()) {
								eventBus.finishEvent(node.getSequence());
							}
							return node;
						}
					}
				}
				int stamp = this.stamp;
				final long readerIndex = this.readerIndex;
				if (readerIndex < eventBus.writerIndex()) {
					node = eventBus.getEvent(readerIndex);
					this.readerIndex = readerIndex + 1;
					// 执行器
					if (node == null || !node.isTrack(track)) {
						continue;
					}
					if (isReorder(node)) {
						if (node.tryIntercept()) {
							// only execute thread return event
//							Debug.info("Work-" + track + " 增加屏障：" + node.getTask());
							_addBarrier(node);
						} else {
//							Debug.info("Work-" + track + " 执行任务：" + node.getTask());
							eventBus.finishEvent(readerIndex);
							return node;
						}
					} else {
//						Debug.info("Work-" + track + " 缓存任务：" + node.getTask());
						if (node.isAloneTrack()) {
							eventBus.finishEvent(readerIndex);
						}
						node.setSequence(readerIndex);
						_addCache(node);
					}
				} else {
					blocked = true;
					// 在读取过程中，是否发生过变化
					if (stamp != this.stamp) {
						blocked = false;
						continue;
					}
					LockSupport.park();
				}
			}
		}

		@Override
		public void run() {
			Node node;
			while ((node = next()) != null) {
				try {
					node.getTask().run();
				} finally {
					completedTasks++;
					node.release(track);
				}
			}
		}

		@Override
		public String toString() {
			return "ReorderWorker-" + track;
		}
	}

	// region Node

	private static final VarHandle VV = XUtils.findVarHandle(AbstractNode.class, "forbids", int.class);

	/**
	 * 节点
	 * <p>
	 * 将任务相关的信息和拦截器融合到一起，尽管没有继承自{@link Interceptor}，但依然实现了拦截器的功能。节点中包含：
	 * <ol>
	 *     <li>任务唯一序列（可能没有）；</li>
	 *     <li>任务；</li>
	 *     <li>任务屏障（可能有多个）；</li>
	 *     <li>任务轨道（可能有多个）；</li>
	 *     <li>拦截器信息。</li>
	 * </ol>
	 * 节点可以以不同的身份被多个线程持有，节点中拦截器的释放变得更加复杂且难以复用，这使得节点也变成了一次性的消耗品。
	 * 在多线程环境下，节点有可能会被多个{@link Walker}持有，持有的形式包括：
	 * <ul>
	 *     <li>执行屏障，此时仅作为屏障，当拦截器释放时，屏障移除。</li>
	 *     <li>缓存节点，当节点无法重排序到屏障之前时，将节点缓存，等待屏障释放后才能开始处理此节点。</li>
	 *     <li>执行任务，此任务由{@link Walker}执行。</li>
	 * </ul>
	 * 同一个节点同时只会以一种形式被一个线程所持有，这三种状态是相互冲突的。
	 * <p>
	 * 同时，节点可以判断一个任务是否能由此线程处理，只有线程轨道和任务轨道重合时，节点才能由此线程处理，否则，忽略这个节点。
	 * <p>
	 * <p>
	 * 注意：如果能顺利解决拦截器释放的问题，可以考虑使用对象池以提升性能。
	 */
	public interface Node {

		/**
		 * 设置任务的唯一序列
		 * <p>
		 * 每一个任务在加入队列时，会给任务分配一个自增的唯一序列。任务移除时，根据唯一序列移除任务。
		 * <p>
		 * 这不是必须的，例如：任务只有一个屏障，那么节点的持有是完全可预测的，此值将被忽略以节省内存。
		 *
		 * @param sequence 任务的唯一序列
		 */
		void setSequence(long sequence);

		/**
		 * 获取任务的唯一序列
		 *
		 * @return 任务的唯一序列
		 */
		long getSequence();

		/**
		 * 获取节点中包含的任务
		 *
		 * @return 等待执行的任务
		 */
		Runnable getTask();

		/**
		 * 任务屏障的数量
		 * <p>
		 * Explorer的任务执行时，需要一个任务屏障，这个方法返回任务屏障数量。
		 *
		 * @return 任务屏障的数量
		 */
		int size();

		/**
		 * 尝试拦截
		 * <p>
		 * 尝试拦截当前线程，并返回是否拦截成功。拦截线程会发生：
		 * <ul>
		 *     <li>拦截成功，那么该线程无法执行此任务，应将此节点作为执行屏障，拦截后续带有相同屏障的节点。</li>
		 *     <li>拦截失败，将由该线程执行此任务，并且在任务执行完成后，释放此拦截器。</li>
		 * </ul>
		 *
		 * @return {@code true}拦截成功，{@code false}拦截失败。
		 * @see Interceptor 拦截器的真正实现
		 */
		boolean tryIntercept();

		/**
		 * 是否拦截
		 * <p>
		 * 当拦截器生效，拦截后续所有节点中拥有相同屏障的节点进行缓存，没有相同屏障的节点重排序运行。当拦截器释放时，
		 * 线程应放弃对于此节点的持有并移除节点。
		 *
		 * @return {@code true}正在拦截，{@code false}拦截器已释放。
		 */
		boolean isIntercepted();

		/**
		 * 返回此节点是否属于这个轨道
		 * <p>
		 * 任务附加的屏障同时决定了任务由哪个线程处理。这个方法用于判断某一个线程能否处理这个节点。
		 *
		 * @param track 轨道
		 * @return {@code true}线程处理此节点，{@code false}线程忽略此节点。
		 */
		boolean isTrack(int track);

		/**
		 * 返回节点是否只由一个线程处理
		 * <p>
		 * 尽管可能线程拥有多个屏障，如果多个屏障最终哈希到一个线程，那么也将由一个线程处理。
		 *
		 * @return {@code true}单线程任务，{@code false}多线程任务。
		 */
		boolean isAloneTrack();

		/**
		 * 唤醒线程
		 * <p>
		 * 唤醒线程的时机：
		 * <ul>
		 *     <li>任务发布，当任务发布到总线后，如果处理此任务的线程（们）处于休眠状态，则唤醒此线程（们）。</li>
		 *     <li>恢复屏障，当缓存节点恢复执行并且节点拦截成功时，唤醒处理此任务的其它线程。</li>
		 * </ul>
		 * 唤醒线程的实现是向该线程发放一个许可，多次/重复唤醒仅发放一个许可，当线程被唤醒后，许可被消耗。
		 * 线程多次被唤醒，仅仅会造成一些性能上的浪费，并不会造成运行时的问题。
		 */
		void weakUp();

		/**
		 * 返回其它节点是否能越过此节点（屏障）重排序执行
		 * <p>
		 * 如果任务屏障完全不同，则可以重排序执行，这对最终的结果不会产生影响。
		 *
		 * @param other 尝试越过此节点的其它节点
		 * @return {@code true}允许越过当前节点重排序运行，{@code false}禁止越过当前节点重排序运行。
		 */
		boolean isReorder(Node other);

		/**
		 * 释放节点（拦截器）
		 * <p>
		 * 任务执行后释放拦截器，可选实现：节点作为一次性的，无需唤醒其它线程，如果使用对象池，需要移除其它线程持有的这个节点。
		 *
		 * @param track 执行任务的轨道
		 */
		void release(int track);
	}

	public abstract static class AbstractNode implements Node {

		protected final int size;

		protected final Runnable task;

		long sequence;

		@Contended
		protected volatile int forbids;

		protected volatile boolean intercepted = true;

		protected AbstractNode(Runnable task, int size) {
			this.size = size;
			this.task = task;
		}

		@Override
		public long getSequence() {
			return sequence;
		}

		@Override
		public void setSequence(long sequence) {
			this.sequence = sequence;
		}

		@Override
		public Runnable getTask() {
			return task;
		}

		@Override
		public int size() {
			return size;
		}

		public boolean tryIntercept() {
			int v;
			do {
				v = forbids;
			} while (!VV.compareAndSet(this, v, v - 1));
			return v > 0;
		}

		@Override
		public boolean isIntercepted() {
			return intercepted;
		}

		@Override
		public void release(int track) {
			this.intercepted = false;
		}
	}

	private class Node1 implements Node {

		protected final Runnable task;

		private final Object fence;

		private final int track;

		public Node1(Runnable task, Object fence) {
			this.task = task;
			this.track = fence.hashCode() % nThreads;
			this.fence = fence;
		}

		@Override
		public void setSequence(long sequence) {
			// do nothing
		}

		@Override
		public long getSequence() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Runnable getTask() {
			return task;
		}

		@Override
		public int size() {
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
		public boolean isTrack(int track) {
			return this.track == track;
		}

		@Override
		public boolean isAloneTrack() {
			return true;
		}

		@Override
		public void weakUp() {
			ReorderedExplorer.this.weakUp(track);
		}

		/**
		 * 判断另一个节点是否能越过当前节点执行
		 *
		 * @param other 后续节点
		 * @return {@code true}可以越过当前栅栏，{@code false}不能越过当前栅栏
		 */
		@Override
		public boolean isReorder(Node other) {
			switch (other.size()) {
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
					for (int i = 0, count = nodeX.size; i < count; i++) {
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

	private class Node2 extends AbstractNode {

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
			this.forbids = track1 == track2 ? 0 : 1;
		}

		@Override
		public void weakUp() {
			ReorderedExplorer.this.weakUp(track1);
			ReorderedExplorer.this.weakUp(track2);
		}

		/**
		 * 判断另一个节点是否能越过当前节点执行
		 *
		 * @param other 后续节点
		 * @return {@code true}可以越过当前栅栏，{@code false}不能越过当前栅栏
		 */
		@Override
		public boolean isReorder(Node other) {
			switch (other.size()) {
				case 1: {
					throw new IllegalStateException();
				}
				case 2: {
					Node2 node2 = (Node2) other;
					return !(node2.fence0.equals(fence0) || node2.fence0.equals(fence1) || node2.fence1.equals(fence0) || node2.fence1.equals(fence1));
				}
				case 3: {
					Node3 node3 = (Node3) other;
					return !(node3.fence0.equals(fence0) || node3.fence0.equals(fence1) || node3.fence1.equals(fence0) || node3.fence1.equals(fence1) || node3.fence2.equals(fence0) || node3.fence2.equals(fence1));
				}
				default: {
					NodeX nodeX = (NodeX) other;
					for (int i = 0, count = nodeX.size; i < count; i++) {
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
		public boolean isTrack(int track) {
			return track1 == track || track2 == track;
		}

		@Override
		public boolean isAloneTrack() {
			return track1 == track2;
		}

//		@Override
//		public void release(int track) {
//			intercepted = false;
//			if (track != track1) {
//				ReorderedExplorer.this.weakUp(track1);
//			}
//			if (track != track2) {
//				ReorderedExplorer.this.weakUp(track2);
//			}
//		}
	}

	private class Node3 extends AbstractNode {

		private final int track0;

		private final Object fence0;

		private final int track1;

		private final Object fence1;

		private final int track2;

		private final Object fence2;

		public Node3(Runnable task, Object fence0, Object fence1, Object fence2) {
			super(task, 3);
			this.track0 = fence0.hashCode() % nThreads;
			this.track1 = fence1.hashCode() % nThreads;
			this.track2 = fence2.hashCode() % nThreads;
			this.fence0 = fence0;
			this.fence1 = fence1;
			this.fence2 = fence2;
			if (this.track0 == this.track1 && this.track0 == this.track2) {
				this.forbids = 0;
			} else if (this.track0 == this.track1 || this.track0 == this.track2 || this.track1 == this.track2) {
				this.forbids = 1;
			} else {
				this.forbids = 2;
			}
		}

		@Override
		public void weakUp() {
			ReorderedExplorer.this.weakUp(track0);
			ReorderedExplorer.this.weakUp(track1);
			ReorderedExplorer.this.weakUp(track2);
		}

		/**
		 * 判断另一个节点是否能越过当前节点执行
		 *
		 * @param other 后续节点
		 * @return {@code true}可以越过当前栅栏，{@code false}不能越过当前栅栏
		 */
		@Override
		public boolean isReorder(Node other) {
			switch (other.size()) {
				case 1: {
					throw new IllegalStateException();
				}
				case 2: {
					Node2 node2 = (Node2) other;
					return !(node2.fence0.equals(fence0) || node2.fence0.equals(fence1) || node2.fence0.equals(fence2) || node2.fence1.equals(fence0) || node2.fence1.equals(fence1) || node2.fence1.equals(fence2));
				}
				case 3: {
					Node3 node3 = (Node3) other;
					return !(node3.fence0.equals(fence0) || node3.fence0.equals(fence1) || node3.fence0.equals(fence2) || node3.fence1.equals(fence0) || node3.fence1.equals(fence1) || node3.fence1.equals(fence2) || node3.fence2.equals(fence0) || node3.fence2.equals(fence1) || node3.fence2.equals(fence2));
				}
				default: {
					NodeX nodeX = (NodeX) other;
					for (int i = 0, count = nodeX.size; i < count; i++) {
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
		public boolean isTrack(int track) {
			return track0 == track || track1 == track || track2 == track;
		}

		@Override
		public boolean isAloneTrack() {
			return track0 == track1 && track0 == track2;
		}

//		@Override
//		public void release(int track) {
//			intercepted = false;
//			if (track != track0) {
//				ReorderedExplorer.this.weakUp(track0);
//			}
//			if (track != track1) {
//				ReorderedExplorer.this.weakUp(track1);
//			}
//			if (track != track2) {
//				ReorderedExplorer.this.weakUp(track2);
//			}
//		}
	}

	private class NodeX extends AbstractNode {

		/**
		 * 任务位置
		 * <p>
		 * 共有{@link #nThreads}条轨道，当前任务所处的轨道位置（可能不止一个）。轨道数量不超过64，所以使用{@code bits}存储。
		 */
		protected long bits;

		private final Object[] fences;

		public NodeX(Runnable task, Object... fences) {
			super(task, fences.length);
			this.fences = fences;
			for (int i = 0; i < size; i++) {
				this.bits |= (1L << fences[i].hashCode() % nThreads);
			}
			this.forbids = Long.bitCount(bits) - 1;
		}

		@Override
		public void weakUp() {
			for (int i = 0; i < nThreads; i++) {
				if ((bits & (1L << i)) != 0) {
					ReorderedExplorer.this.weakUp(i);
				}
			}
		}

		@Override
		public boolean isTrack(int track) {
			return (bits & (1L << track)) != 0;
		}

		@Override
		public boolean isAloneTrack() {
			return Long.bitCount(bits) <= 1;
		}

		/**
		 * 判断另一个节点是否能越过当前节点执行
		 *
		 * @param other 后续节点
		 * @return {@code true}可以越过当前栅栏，{@code false}不能越过当前栅栏
		 */
		@Override
		public boolean isReorder(Node other) {
			switch (other.size()) {
				case 1: {
					throw new IllegalStateException();
				}
				case 2: {
					Node2 node = (Node2) other;
					for (int i = 0; i < size; i++) {
						Object fence = fences[i];
						if (fence.equals(node.fence0) || fence.equals(node.fence1)) {
							return false;
						}
					}
					return true;
				}
				case 3: {
					Node3 node = (Node3) other;
					for (int i = 0; i < size; i++) {
						Object fence = fences[i];
						if (fence.equals(node.fence0) || fence.equals(node.fence1) || fence.equals(node.fence2)) {
							return false;
						}
					}
					return true;
				}
				default: {
					NodeX node = (NodeX) other;
					for (int i = 0, count = node.size; i < count; i++) {
						Object v = node.fences[i];
						for (int j = 0; j < size; j++) {
							if (v.equals(fences[j])) {
								return false;
							}
						}
					}
					return true;
				}
			}
		}

//		@Override
//		public void release(int track) {
//			intercepted = false;
//			for (int i = 0; i < nThreads; i++) {
//				if ((bits & (1L << i)) != 0 && i != track) {
//					ReorderedExplorer.this.weakUp(i);
//				}
//			}
//		}
	}
	// endregion

	/**
	 * 守望线程
	 */
	public class Watcher implements Runnable {

		volatile Runnable task = null;

		@Override
		public void run() {
			while (state < TERMINATED) {
				if (task != null) {
					task.run();
				}
				// park 1 ms
				LockSupport.parkNanos(1_000_000);
			}
		}
	}

	public class CloseFuture implements Future<Object> {

		volatile boolean cancel = false;

		Predicate<ExplorerService> tester;

		public CloseFuture(Predicate<ExplorerService> tester) {
			this.tester = tester;
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			cancel = true;
			return true;
		}

		@Override
		public boolean isCancelled() {
			return cancel;
		}

		@Override
		public boolean isDone() {
			return false;
		}

		@Override
		public Object get() throws InterruptedException, ExecutionException {
			while (!tester.test(ReorderedExplorer.this)) {
				Thread.sleep(1);
			}
			return null;
		}

		@Override
		public Object get(long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			throw new UnsupportedOperationException();
		}
	}
}
