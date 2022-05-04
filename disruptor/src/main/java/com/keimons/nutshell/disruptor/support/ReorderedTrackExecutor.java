package com.keimons.nutshell.disruptor.support;

import com.keimons.nutshell.disruptor.AbstractTrackExecutor;
import com.keimons.nutshell.disruptor.RejectedTrackExecutionHandler;
import com.keimons.nutshell.disruptor.TrackBarrier;
import com.keimons.nutshell.disruptor.internal.BitsTrackBarrier;
import com.keimons.nutshell.disruptor.internal.BitsTrackEventBus;
import com.keimons.nutshell.disruptor.internal.EventBus;
import jdk.internal.vm.annotation.Contended;

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
public class ReorderedTrackExecutor extends AbstractTrackExecutor {

	/**
	 * 事件总线
	 * <p>
	 * 所有任务都发布在事件总线上，如果事件总线不能发布任务，任务发布失败，则队列已满。
	 */
	private final EventBus<Event> eventBus;

	/**
	 * 任务执行器
	 */
	private final ReorderedTrackWorker[] executors;

	public ReorderedTrackExecutor(String name, int nThreads, int capacity, RejectedTrackExecutionHandler rejectedHandler) {
		super(name, nThreads, rejectedHandler);
		eventBus = new BitsTrackEventBus<>(Event::new, capacity);
		executors = new ReorderedTrackWorker[nThreads];
		for (int i = 0; i < nThreads; i++) {
			ReorderedTrackWorker executor = new ReorderedTrackWorker(i, threadFactory);
			executor.thread.start();
			executors[i] = executor;
		}
	}

	@Override
	public void execute(Runnable task, Object fence) {
		Event event = eventBus.borrowEvent();
		event.init(task, fence);
		eventBus.publishEvent(event);
		weakUp(event);
	}

	private void weakUp(Event event) {
		for (int i = 0; i < nThreads; i++) {
			if (event.isTrack(i)) {
				ReorderedTrackWorker worker = executors[i];
				if (worker.parked) {
					worker.parked = false;
					LockSupport.unpark(worker.thread);
				}
			}
		}
	}

	private void weakUp(Event event, int track) {
		if (!event.isSingle(track)) {
			weakUp(event);
		}
	}

	public void execute(Runnable task, Object fence0, Object fence1) {
		Event event = eventBus.borrowEvent();
		event.init(task, fence0, fence1);
		eventBus.publishEvent(event);
	}

	public void execute(Runnable task, Object fence0, Object fence1, Object fence2) {
		Event event = eventBus.borrowEvent();
		event.init(task, fence0, fence1, fence2);
		eventBus.publishEvent(event);
	}

	public void execute(Runnable task, Object... fences) {
		Event event = eventBus.borrowEvent();
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

		@Contended
		protected final Thread thread;

		protected final int track;

		private int interceptIndex;

		private int barrierIndex;

		private long readerIndex;

		/**
		 * 拦截队列
		 * <p>
		 * 当前线程中被屏障拦截的事件，直接缓存在执行器本地，等待拦截器释放后，再执行缓存的消息。
		 */
		private Event[] intercepted = new Event[8];

		/**
		 * 执行屏障
		 * <p>
		 * 事件总线上的事件，即使是由当前线程执行，也并不一定可以立即处理。
		 */
		private Event[] barriers = new Event[8];

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

		private void _add0(Event event) {
			if (interceptIndex >= intercepted.length) {
				Event[] tmp = new Event[interceptIndex << 1];
				System.arraycopy(intercepted, 0, tmp, 0, interceptIndex);
				intercepted = tmp;
			}
			intercepted[interceptIndex++] = event;
		}

		private void _add1(Event event) {
			if (barrierIndex >= barriers.length) {
				Event[] tmp = new Event[barrierIndex << 1];
				System.arraycopy(barriers, 0, tmp, 0, barrierIndex);
				barriers = tmp;
			}
			barriers[barrierIndex++] = event;
		}

		private void _remove0(int index) {
			for (int i = index, limit = interceptIndex - 1; i < limit; i++) {
				intercepted[i] = intercepted[i + 1];
			}
			intercepted[interceptIndex] = null;
			interceptIndex--;
		}

		private void _remove1(int index) {
			for (int i = index, limit = barrierIndex - 1; i < limit; i++) {
				barriers[i] = barriers[i + 1];
			}
			barriers[barrierIndex] = null;
			barrierIndex--;
		}

		private boolean isReorder(Event event) {
			for (int i = 0; i < barrierIndex; i++) {
				if (event.reorder(track, barriers[i])) {
					return false;
				}
			}
			return true;
		}

		private Event next() {
			while (true) {
				for (int i = 0; i < barrierIndex; i++) {
					Event event = barriers[i];
					if (!event.isIntercepted()) {
//						Debug.info("Work-" + track + " 移除屏障：" + event.task);
						_remove1(i);
					}
				}
				for (int i = 0; i < interceptIndex; i++) {
					Event event = intercepted[i];
					if (isReorder(event)) {
						if (event.tryIntercept()) {
//							Debug.info("Work-" + track + " 恢复屏障：" + event.task);
							_add1(event);
						} else {
//							Debug.info("Work-" + track + " 恢复任务：" + event.task);
							_remove0(i);
							return event;
						}
					}
				}
				long readerIndex = this.readerIndex;
				if (readerIndex < eventBus.writerIndex()) {
					Event event = eventBus.getEvent(readerIndex);
					this.readerIndex = readerIndex + 1;
					if (event.isTrack(1L << track)) {
						if (isReorder(event)) {
							if (event.tryIntercept()) {
								// only execute thread return event
//								Debug.info("Work-" + track + " 增加屏障：" + event.task);
								_add1(event);
							} else {
//								Debug.info("Work-" + track + " 执行任务：" + event.task);
								eventBus.finishEvent(readerIndex);
								return event;
							}
						} else {
//							Debug.info("Work-" + track + " 缓存任务：" + event.task);
							eventBus.finishEvent(readerIndex);
							_add0(event);
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
						Event event = null;
						try {
							event = next();
							if (event == null) {
								parked = true;
								LockSupport.park();
							} else {
								event.task.run();
							}
						} catch (Throwable e) {
							// ignore
						} finally {
							if (event != null) {
								event.release(track);
								eventBus.returnEvent(event);
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

	public class Event extends BitsTrackBarrier {

		public Runnable task;

		public Event() {
			super(nThreads);
		}

		public void init(Runnable task, Object fence) {
			this.task = task;
			this.init(fence);
		}

		public void init(Runnable task, Object fence0, Object fence1) {
			this.task = task;
			this.init(fence0, fence1);
		}

		public void init(Runnable task, Object fence0, Object fence1, Object fence2) {
			this.task = task;
			this.init(fence0, fence1, fence2);
		}

		public void init(Runnable task, Object... fences) {
			this.task = task;
			this.init(fences);
		}

		public void release(int track) {
			task = null;
			intercepted = false;
			weakUp(this, track);
			release();
		}
	}
}
