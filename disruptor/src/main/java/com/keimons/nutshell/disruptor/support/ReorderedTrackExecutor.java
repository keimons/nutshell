package com.keimons.nutshell.disruptor.support;

import com.keimons.nutshell.disruptor.AbstractTrackExecutor;
import com.keimons.nutshell.disruptor.Debug;
import com.keimons.nutshell.disruptor.RejectedTrackExecutionHandler;
import com.keimons.nutshell.disruptor.TrackBarrier;
import com.keimons.nutshell.disruptor.support.event.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 重新排序轨道执行器
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class ReorderedTrackExecutor extends AbstractTrackExecutor {

	private final EventBus<?> eventBus;

	/**
	 * 任务执行器
	 */
	private final ReorderedTrackWorker[] executors;

	public ReorderedTrackExecutor(String name, int nThreads, RejectedTrackExecutionHandler rejectedHandler) {
		super(name, nThreads, rejectedHandler);
		eventBus = new EventBus<>(1024);
		executors = new ReorderedTrackWorker[nThreads];
		for (int i = 0; i < nThreads; i++) {
			ReorderedTrackWorker executor = new ReorderedTrackWorker(i, threadFactory);
			executor.thread.start();
			executors[i] = executor;
		}
	}

	public void execute(Runnable task, Object fence) {
		TrackBarrier barrier = new BitTrackBarrier(nThreads);
		barrier.init(fence);
		execute(barrier, task);
	}

	public void execute(Runnable task, Object fence0, Object fence1) {
		TrackBarrier barrier = new BitTrackBarrier(nThreads);
		barrier.init(fence0, fence1);
		execute(barrier, task);
	}

	public void execute(Runnable task, Object fence0, Object fence1, Object fence2) {
		TrackBarrier barrier = new BitTrackBarrier(nThreads);
		barrier.init(fence0, fence1, fence2);
		execute(barrier, task);
	}

	public void execute(Runnable task, Object... fences) {
		TrackBarrier barrier = new BitTrackBarrier(nThreads);
		barrier.init(fences);
		execute(barrier, task);
	}

	@Override
	public void execute(TrackBarrier barrier, Runnable task) {
		if (!running) {
			rejectedHandler.rejectedExecution(barrier, task, this);
		}
		eventBus.publish(barrier, task);
	}

	@Override
	public Future<?> submit(TrackBarrier barrier, Runnable task) {
		RunnableFuture<Void> future = new FutureTask<>(task, null);
		execute(barrier, future);
		return future;
	}

	@Override
	public <T> Future<T> submit(TrackBarrier barrier, Callable<T> task) {
		FutureTask<T> future = new FutureTask<>(task);
		execute(barrier, future);
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
	 * 消息执行队列
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
		protected long readerIndex;
		List<EventBus.Node> nodes = new ArrayList<>();
		List<EventBus.Node> barriers = new ArrayList<>();

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

		protected EventBus.Node next() throws InterruptedException {
			while (true) {
				barriers.removeIf(node -> !node.interceptor.isIntercepted());
				for (int i = 0; i < nodes.size(); i++) {
					EventBus.Node node = nodes.get(i);
					if (barriers.stream().allMatch(barrier -> node.barrier.reorder(track, barrier.barrier))) {
						if (node.interceptor.tryIntercept()) {
							Debug.info("Work-" + track + " 增加屏障：" + node.task.toString());
							barriers.add(node);
						} else {
							nodes.remove(i);
							Debug.info("Work-" + track + " 恢复任务：" + node.task.toString());
							return node;
						}
					}
				}
				if (readerIndex < eventBus.getWriterIndex()) {
					EventBus.Node node = eventBus.get(readerIndex);
					if (node.barrier.isTrack(1L << track)) {
						readerIndex++;
						if (barriers.stream().allMatch(barrier -> node.barrier.reorder(track, barrier.barrier))) {
							if (node.interceptor.tryIntercept()) {
								Debug.info("Work-" + track + " 增加屏障：" + node.task.toString());
								barriers.add(node);
							} else {
								Debug.info("Work-" + track + " 执行任务：" + node.task.toString());
								return node;
							}
						} else {
							Debug.info("Work-" + track + " 缓存任务：" + node.task.toString());
							nodes.add(node);
						}
					} else {
						readerIndex++;
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
						EventBus.Node runnable = null;
						try {
							runnable = next();
							beforeExecute();
							runnable.task.run();
							runnable.interceptor.release();
						} catch (Throwable e) {
							// ignore
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
}
