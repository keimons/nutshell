package com.keimons.nutshell.dispenser.support;

import com.keimons.nutshell.dispenser.AbstractHashExecutor;
import com.keimons.nutshell.dispenser.RejectedHashExecutionHandler;

import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 哈希队列执行器
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class HashQueueExecutor extends AbstractHashExecutor {

	/**
	 * 任务执行器
	 */
	private final AbstractExecutor[] executors;

	public HashQueueExecutor(String name, int nThreads, RejectedHashExecutionHandler rejectedHandler) {
		super(name, nThreads, rejectedHandler);
		executors = new AbstractExecutor[nThreads];
		for (int i = 0; i < nThreads; i++) {
			UnboundedExecutor executor = new UnboundedExecutor();
			executor.thread.start();
			executors[i] = executor;
		}
	}

	@Override
	public void execute(int hash, Runnable task) {
		executors[hash % nThreads].execute(task);
	}

	@Override
	public void executeNow(int hash, Runnable task) {
		executors[hash % nThreads].executeNow(task);
	}

	@Override
	public Future<?> submit(int hash, Runnable task) {
		return executors[hash % nThreads].submit(task);
	}

	@Override
	public Future<?> submitNow(int hash, Runnable task) {
		return executors[hash % nThreads].submitNow(task);
	}

	@Override
	public <T> Future<T> submit(int hash, Callable<T> task) {
		return executors[hash % nThreads].submit(task);
	}

	@Override
	public <T> Future<T> submitNow(int hash, Callable<T> task) {
		return executors[hash % nThreads].submitNow(task);
	}

	@Override
	public void shutdown() {
		for (AbstractExecutor executor : executors) {
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
	private abstract class AbstractExecutor implements Runnable {

		/**
		 * 是否执行中
		 */
		protected volatile boolean running = true;

		protected final Thread thread;

		/**
		 * 执行器
		 * <p>
		 * 消息的真正执行者
		 */
		public AbstractExecutor() {
			this.thread = threadFactory.newThread(this);
		}

		protected abstract Runnable next() throws InterruptedException;

		protected void after() {

		}

		/**
		 * 增加一个任务
		 *
		 * @param task 队尾
		 */
		public abstract void execute(Runnable task);

		/**
		 * 增加一个任务
		 *
		 * @param task 队尾
		 */
		public void executeNow(Runnable task) {
			throw new UnsupportedOperationException();
		}

		/**
		 * 增加一个任务
		 *
		 * @param task 队尾
		 */
		public abstract Future<?> submit(Runnable task);

		/**
		 * 增加一个任务
		 *
		 * @param task 队尾
		 */
		public Future<?> submitNow(Runnable task) {
			throw new UnsupportedOperationException();
		}

		/**
		 * 增加一个任务
		 *
		 * @param task 队尾
		 * @return 是否成功
		 */
		public abstract <T> FutureTask<T> submit(Callable<T> task);

		/**
		 * 增加一个任务
		 *
		 * @param task 队尾
		 * @return 是否成功
		 */
		public <T> FutureTask<T> submitNow(Callable<T> task) {
			throw new UnsupportedOperationException();
		}

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
							runnable.run();
							after();
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
	 *
	 * @author houyn[monkey@keimons.com]
	 * @version 1.0
	 * @since 11
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
		public void execute(Runnable task) {
			queue.offerLast(task);
		}

		@Override
		public void executeNow(Runnable task) {
			queue.offerFirst(task);
		}

		@Override
		public Future<?> submit(Runnable task) {
			RunnableFuture<Void> future = new FutureTask<>(task, null);
			execute(future);
			return future;
		}

		@Override
		public Future<?> submitNow(Runnable task) {
			RunnableFuture<Void> future = new FutureTask<>(task, null);
			executeNow(future);
			return future;
		}

		@Override
		public <T> FutureTask<T> submit(Callable<T> task) {
			FutureTask<T> future = new FutureTask<>(task);
			execute(future);
			return future;
		}

		@Override
		public <T> FutureTask<T> submitNow(Callable<T> task) {
			FutureTask<T> future = new FutureTask<>(task);
			executeNow(future);
			return future;
		}

		@Override
		public void shutdown() {
			running = false;
			// 增加一个空消息，保证能够正常结束任务
			queue.add(() -> {
			});
		}
	}

	/**
	 * 定长队列执行器（支持立即提交）
	 *
	 * @author houyn[monkey@keimons.com]
	 * @version 1.0
	 * @since 11
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
		protected void after() {
			notFull.signal();
		}

		@Override
		public void execute(Runnable task) {
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
		public Future<?> submit(Runnable task) {
			RunnableFuture<Void> future = new FutureTask<>(task, null);

			return future;
		}

		@Override
		public <T> FutureTask<T> submit(Callable<T> task) {
			FutureTask<T> future = new FutureTask<>(task);
			return future;
		}

		@Override
		public void shutdown() {
			running = false;
			// 增加一个空消息，保证能够正常结束任务
			// TODO LockSupport.unpark(thread);
		}
	}
}
