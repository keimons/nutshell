package com.keimons.nutshell.dispenser;

import java.util.concurrent.*;

/**
 * 共享队列执行器
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class SharedQueueExecutor extends AbstractHashExecutor {

	/**
	 * 执行器
	 */
	private final ThreadPoolExecutor service;

	private final LinkedBlockingDeque<Runnable> sharedQueue;

	public SharedQueueExecutor(String name, int nThreads, RejectedHashExecutionHandler rejectedHandler) {
		super(name, nThreads, rejectedHandler);
		sharedQueue = new LinkedBlockingDeque<>(4096);
		service = new ThreadPoolExecutor(nThreads,
				nThreads,
				0L,
				TimeUnit.MILLISECONDS,
				sharedQueue,
				new InternalRejectedExecutionHandler()
		);
		// 注意这一步是不可少的，因为nutshell有可能会提交插队的任务。
		// 必须保证线程池内部已经初始化线程了。
		// 由于{@link ThreadPoolExecutor}采用惰性初始化，需要先对其进行初始化。
		for (int i = 0; i < nThreads; i++) {
			service.execute(() -> {
			});
		}
	}

	@Override
	public void execute(int hash, Runnable task) {
		try {
			service.execute(task);
		} catch (InternalException e) {
			if (blockingCaller) {
				try {
					for (; ; ) {
						if (service.isShutdown()) {
							rejectedHandler.rejectedExecution(hash, task, this);
							return;
						} else if (sharedQueue.offerLast(task, 1000000000L, TimeUnit.NANOSECONDS)) {
							break;
						}
					}
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
			} else {
				rejectedHandler.rejectedExecution(hash, task, this);
			}
		}
	}

	@Override
	public void executeNow(int hash, Runnable task) {
		if (task == null) {
			throw new NullPointerException();
		}
		if (service.isShutdown()) {
			rejectedHandler.rejectedExecution(hash, task, this);
			return;
		}
		if (!sharedQueue.offerFirst(task)) {
			if (blockingCaller) {
				try {
					sharedQueue.putFirst(task);
				} catch (InterruptedException e) {
					// ignore exception & restore interrupt
					Thread.currentThread().interrupt();
				}
			} else {
				rejectedHandler.rejectedExecution(hash, task, this);
			}
		}
	}

	@Override
	public Future<?> submit(int hash, Runnable task) {
		return service.submit(task);
	}

	public Future<?> submitNow(int hash, Runnable task) {
		if (task == null) {
			throw new NullPointerException();
		}
		RunnableFuture<?> future = new FutureTask<>(task, null);
		if (service.isShutdown()) {
			rejectedHandler.rejectedExecution(hash, task, this);
		} else {
			sharedQueue.offerFirst(future);
			if (!sharedQueue.offerFirst(task)) {
				if (blockingCaller) {
					try {
						sharedQueue.putFirst(task);
					} catch (InterruptedException e) {
						// ignore exception & restore interrupt
						Thread.currentThread().interrupt();
					}
				} else {
					rejectedHandler.rejectedExecution(hash, task, this);
				}
			}
		}
		return future;
	}

	@Override
	public <T> Future<T> submit(int hash, Callable<T> task) {
		return service.submit(task);
	}

	public <T> Future<T> submitNow(int hash, Callable<T> task) {
		if (task == null) {
			throw new NullPointerException();
		}
		RunnableFuture<T> future = new FutureTask<>(task);
		sharedQueue.offerFirst(future);
		return future;
	}

	@Override
	public void shutdown() {
		service.shutdown();
	}

	/**
	 * 关联{@link RejectedExecutionHandler}和{@link RejectedHashExecutionHandler}
	 */
	private static final class InternalRejectedExecutionHandler implements RejectedExecutionHandler {

		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
			throw new InternalException();
		}
	}

	private static final class InternalException extends RuntimeException {

	}
}
