package com.keimons.nutshell.disruptor.support;

import com.keimons.nutshell.disruptor.*;
import com.keimons.nutshell.disruptor.internal.BitsTrackBarrier;
import com.keimons.nutshell.disruptor.internal.BitsTrackEventBus;
import com.keimons.nutshell.disruptor.internal.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

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
public class ReorderedTrackExecutor extends AbstractTrackExecutor {

	/**
	 * 事件总线
	 * <p>
	 * 所有任务都发布在事件总线上，如果事件总线不能发布任务，任务发布失败，则队列已满。
	 */
	private final EventBus<Node> eventBus;

	/**
	 * 任务执行器
	 */
	private final ReorderedTrackWorker[] executors;

	public ReorderedTrackExecutor(String name, int nThreads, RejectedTrackExecutionHandler rejectedHandler) {
		super(name, nThreads, rejectedHandler);
		eventBus = new BitsTrackEventBus<>(Node::new, 1024);
		executors = new ReorderedTrackWorker[nThreads];
		for (int i = 0; i < nThreads; i++) {
			ReorderedTrackWorker executor = new ReorderedTrackWorker(i, threadFactory);
			executor.thread.start();
			executors[i] = executor;
		}
	}

	@Override
	public void execute(Runnable task, Object fence) {
		Node event = eventBus.borrowEvent();
		event.init(task, fence);
		eventBus.publishEvent(event);
	}

	public void execute(Runnable task, Object fence0, Object fence1) {
		Node event = eventBus.borrowEvent();
		event.init(task, fence0, fence1);
		eventBus.publishEvent(event);
	}

	public void execute(Runnable task, Object fence0, Object fence1, Object fence2) {
		Node event = eventBus.borrowEvent();
		event.init(task, fence0, fence1, fence2);
		eventBus.publishEvent(event);
	}

	public void execute(Runnable task, Object... fences) {
		Node event = eventBus.borrowEvent();
		event.init(task, fences);
		eventBus.publishEvent(event);
	}

	@Override
	public Future<?> submit(Runnable task, TrackBarrier barrier) {
		RunnableFuture<Void> future = new FutureTask<>(task, null);
		execute(future, barrier);
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

		protected final Thread thread;

		protected final int track;
		/**
		 * 主锁
		 */
		protected final ReentrantLock lock = new ReentrantLock();
		/**
		 * 等待条件
		 */
		protected final Condition notFull = lock.newCondition();
		/**
		 * 拦截队列
		 * <p>
		 * 当前线程中被屏障拦截的事件，直接缓存在执行器本地，等待拦截器释放后，再执行缓存的消息。
		 */
		final List<Node> intercepted = new ArrayList<>();
		/**
		 * 执行屏障
		 * <p>
		 * 事件总线上的事件，即使是由当前线程执行，也并不一定可以立即处理。
		 */
		final List<Node> barriers = new ArrayList<>();
		protected long readerIndex;

		/**
		 * 执行器
		 * <p>
		 * 消息的真正执行者
		 */
		public ReorderedTrackWorker(int track, ThreadFactory threadFactory) {
			this.track = track;
			this.thread = threadFactory.newThread(this);
		}

		/**
		 * 拒绝一个任务
		 *
		 * @param barrier 执行屏障
		 * @param task    任务
		 * @param last    是否队尾
		 */
		protected void reject(TrackBarrier barrier, Runnable task, boolean last) {
			// 检测线程池是否已经关闭，如果线程池关闭，则直接调用拒绝策略
			// 是否阻塞提交者并等待空余位置，如果是，那么阻塞提交者
			if (running && blockingCaller) {
				try {
					// 检测当前线程是否被打断，如果被打断了，那么什么都不处理。
					lock.lockInterruptibly();
					try {
						while (last ? !offerLast(task) : !offerFirst(task)) {
							notFull.await();
							// 线程被唤醒后，先检查线程池是否关闭，线程池关闭时，也会唤醒所有等待中的线程
							if (!running) {
								rejectedHandler.rejectedExecution(barrier, task, ReorderedTrackExecutor.this);
								return;
							}
						}
					} finally {
						lock.unlock();
					}
				} catch (InterruptedException ex) {
					// 回复被打断的状态
					Thread.currentThread().interrupt();
				}
			} else {
				rejectedHandler.rejectedExecution(barrier, task, ReorderedTrackExecutor.this);
			}
		}

		protected void beforeExecute() {
			try {
				lock.lockInterruptibly();
				try {
					notFull.signal();
				} finally {
					lock.unlock();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		protected Node next() throws InterruptedException {
			while (true) {
				barriers.removeIf(node -> !node.interceptor.isIntercepted());
				for (int i = 0; i < intercepted.size(); i++) {
					Node node = intercepted.get(i);
					if (barriers.stream().allMatch(barrier -> node.barrier.reorder(track, barrier.barrier))) {
						if (node.interceptor.tryIntercept()) {
							Debug.info("Work-" + track + " 恢复屏障：" + node.task.toString());
							barriers.add(node);
						} else {
							intercepted.remove(i);
							Debug.info("Work-" + track + " 恢复任务：" + node.task.toString());
							return node;
						}
					}
				}
				if (readerIndex < eventBus.getWriterIndex()) {
					long readerIndex = this.readerIndex;
					Node node = eventBus.getEvent(readerIndex);
					this.readerIndex = readerIndex + 1;
					if (node.barrier.isTrack(1L << track)) {
						if (barriers.stream().allMatch(barrier -> node.barrier.reorder(track, barrier.barrier))) {
							if (node.interceptor.tryIntercept()) {
								Debug.info("Work-" + track + " 增加屏障：" + node.task.toString());
								// only execute thread return event
								barriers.add(node);
							} else {
								Debug.info("Work-" + track + " 执行任务：" + node.task.toString());
								eventBus.returnEvent(readerIndex);
								return node;
							}
						} else {
							Debug.info("Work-" + track + " 缓存任务：" + node.task.toString());
							eventBus.returnEvent(readerIndex);
							intercepted.add(node);
						}
					}
				}
				Thread.yield();
			}
		}

		protected boolean offerFirst(Runnable task) {
			return false;
		}

		protected boolean offerLast(Runnable task) {
			return false;
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
						Node runnable = null;
						try {
							runnable = next();
							beforeExecute();
							runnable.task.run();
						} catch (Throwable e) {
							// ignore
						} finally {
							if (runnable != null) {
								runnable.release();
							}
						}
					}
				} catch (Throwable e) {
					// ignore
				}
			}
		}

		@Override
		public String toString() {
			return "ReorderTrackWorker-" + track;
		}
	}

	public class Node {

		public Runnable task;

		public Interceptor interceptor = new ReorderedInterceptor();

		public TrackBarrier barrier = new BitsTrackBarrier(nThreads);

		public void init(Runnable task, Object fence) {
			this.task = task;
			this.barrier.init(fence);
			this.interceptor.init(barrier.intercept());
		}

		public void init(Runnable task, Object fence0, Object fence1) {
			this.task = task;
			this.barrier.init(fence0, fence1);
			this.interceptor.init(barrier.intercept());
		}

		public void init(Runnable task, Object fence0, Object fence1, Object fence2) {
			this.task = task;
			this.barrier.init(fence0, fence1, fence2);
			this.interceptor.init(barrier.intercept());
		}

		public void init(Runnable task, Object... fences) {
			this.task = task;
			this.barrier.init(fences);
			this.interceptor.init(barrier.intercept());
		}

		public void release() {
			task = null;
			interceptor.release();
			barrier.release();
		}
	}
}
