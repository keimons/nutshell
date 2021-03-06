package com.keimons.nutshell.explorer.support;

import com.keimons.nutshell.explorer.AbstractExplorerService;
import com.keimons.nutshell.explorer.ConsumerFuture;
import com.keimons.nutshell.explorer.RejectedExplorerHandler;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 哈希队列执行器
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
public class QueueExplorer extends AbstractExplorerService {

	/**
	 * 任务执行器
	 */
	private final AbstractExecutor[] executors;

	public QueueExplorer(String name, int nThreads, RejectedExplorerHandler rejectedHandler) {
		super(name, nThreads, rejectedHandler, Executors.defaultThreadFactory());
		executors = new AbstractExecutor[nThreads];
		for (int i = 0; i < nThreads; i++) {
			UnboundedExecutor executor = new UnboundedExecutor();
			executor.thread.start();
			executors[i] = executor;
		}
	}

	@Override
	public void execute(Runnable task, Object fence) {
		if (!running) {
			rejectedHandler.rejectedExecution(this, task, fence);
		}
		executors[fence.hashCode() % nThreads].execute(task, fence);
	}

	@Override
	public void executeNow(Runnable task, Object fence) {
		if (!running) {
			rejectedHandler.rejectedExecution(this, task, fence);
		}
		executors[fence.hashCode() % nThreads].executeNow(task, fence);
	}

	@Override
	public Future<?> submit(Runnable task, Object fence) {
		RunnableFuture<Void> future = new FutureTask<>(task, null);
		execute(future, fence);
		return future;
	}

	@Override
	public Future<?> submitNow(Runnable task, Object fence) {
		RunnableFuture<Void> future = new FutureTask<>(task, null);
		executeNow(future, fence);
		return future;
	}

	@Override
	public <T> Future<T> submit(Callable<T> task, Object fence) {
		FutureTask<T> future = new FutureTask<>(task);
		execute(future, fence);
		return future;
	}

	@Override
	public <T> Future<T> submitNow(Callable<T> task, Object fence) {
		FutureTask<T> future = new FutureTask<>(task);
		executeNow(future, fence);
		return future;
	}

	@Override
	public boolean isShutdown() {
		return !running;
	}

	@Override
	public void close() {

	}

	@Override
	public void close(RunnableFuture<?> onClose) {

	}

	@Override
	public void shutdown(ConsumerFuture<List<Runnable>> runnable) {
		running = false;
		for (AbstractExecutor executor : executors) {
			executor.shutdown();
		}
	}

	/**
	 * 消息执行队列
	 *
	 * @author houyn[monkey@keimons.com]
	 * @version 1.0
	 * @since 17
	 **/
	private abstract class AbstractExecutor implements Runnable {

		protected final Thread thread;

		/**
		 * 主锁
		 */
		protected final ReentrantLock lock = new ReentrantLock();

		/**
		 * 等待条件
		 */
		protected final Condition notFull = lock.newCondition();

		/**
		 * 执行器
		 * <p>
		 * 消息的真正执行者
		 */
		public AbstractExecutor() {
			this.thread = threadFactory.newThread(this);
		}

		/**
		 * 增加一个任务
		 *
		 * @param task  队尾
		 * @param fence 执行屏障
		 */
		public void execute(Runnable task, Object fence) {
			if (!offerLast(task)) {
				reject(true, task, fence);
			}
		}

		/**
		 * 增加一个任务
		 *
		 * @param task  队尾
		 * @param fence 执行屏障
		 */
		public void executeNow(Runnable task, Object fence) {
			if (!offerFirst(task)) {
				reject(false, task, fence);
			}
		}

		/**
		 * 拒绝一个任务
		 *
		 * @param last  是否队尾
		 * @param task  任务
		 * @param fence 执行屏障
		 */
		protected void reject(boolean last, Runnable task, Object fence) {
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
								rejectedHandler.rejectedExecution(QueueExplorer.this, task, fence);
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
				rejectedHandler.rejectedExecution(QueueExplorer.this, task, fence);
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

		protected abstract Runnable next() throws InterruptedException;

		protected abstract boolean offerFirst(Runnable task);

		protected abstract boolean offerLast(Runnable task);

		/**
		 * 关闭线程
		 */
		public abstract void shutdown();

		@Override
		public void run() {
			while (running) {
				try {
					while (running) {
						Runnable runnable = null;
						try {
							runnable = next();
							beforeExecute();
							runnable.run();
						} catch (Throwable e) {
							// ignore
						}
					}
				} catch (Throwable e) {
					// ignore
				}
			}
		}
	}

	/**
	 * 无界队列执行器
	 * <p>
	 * 无界队列不考虑插队失败的情况，当发生插队失败，是直接
	 *
	 * @author houyn[monkey@keimons.com]
	 * @version 1.0
	 * @since 17
	 **/
	private final class UnboundedExecutor extends AbstractExecutor {

		/**
		 * 线程安全的阻塞队列
		 */
		private final BlockingDeque<Runnable> queue;

		/**
		 * 执行器
		 * <p>
		 * 消息的真正执行者
		 */
		public UnboundedExecutor() {
			this.queue = new LinkedBlockingDeque<>();
		}

		@Override
		protected Runnable next() throws InterruptedException {
			return queue.take();
		}

		@Override
		protected boolean offerFirst(Runnable task) {
			return queue.offerFirst(task);
		}

		@Override
		protected boolean offerLast(Runnable task) {
			return queue.offerLast(task);
		}

		@Override
		public void shutdown() {
			try {
				lock.lockInterruptibly();
				try {
					notFull.signal();
					notFull.signalAll();
				} finally {
					lock.unlock();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	/**
	 * 定长队列执行器（支持立即提交）
	 *
	 * @author houyn[monkey@keimons.com]
	 * @version 1.0
	 * @since 17
	 **/
	private class FixedExecutor extends AbstractExecutor {

		private final ReentrantLock lock = new ReentrantLock(true);

		private final Condition notFull = lock.newCondition();

		/**
		 * 线程安全的阻塞队列
		 */
		private final Runnable[] queue;

		private final int length;

		private volatile int writerIndex;

		private volatile int readerIndex;

		public FixedExecutor(int length) {
			this.length = length;
			this.queue = new Runnable[length];
		}

		@Override
		protected Runnable next() throws InterruptedException {
			lock.lock();
			try {

			} finally {
				lock.unlock();
			}
			int index = readerIndex;
			Runnable runnable = queue[index];
//			unsafe.putOrderedInt();
			readerIndex++;
			return runnable;
		}

		@Override
		protected boolean offerFirst(Runnable task) {
			return false;
		}

		@Override
		protected boolean offerLast(Runnable task) {
			return false;
		}

		@Override
		protected void beforeExecute() {
			notFull.signal();
		}

		@Override
		public void execute(Runnable task, Object fence) {
			lock.lock();
			try {
				for (; ; ) {
					// 判断队列是否已满，队列已满则不允许继续添加
					if (writerIndex - readerIndex >= length) {
						if (!blockingCaller) {
							return;
						} else {
							notFull.await();
							continue;
						}
					}
				}
			} catch (InterruptedException e) {
				Thread.getDefaultUncaughtExceptionHandler().uncaughtException(thread, e);
				// ignore
			} finally {
				lock.unlock();
			}
		}

		@Override
		public void shutdown() {
			running = false;
			// 增加一个空消息，保证能够正常结束任务
			// TODO LockSupport.unpark(thread);
		}
	}
}
