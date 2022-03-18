package com.keimons.nutshell.dispenser.support;

import com.keimons.nutshell.dispenser.BlockingCallerHandler;
import com.keimons.nutshell.dispenser.HashExecutor;
import com.keimons.nutshell.dispenser.RejectedHashExecutionHandler;

import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 共享队列执行器
 * <p>
 * 共享队列执行器是{@link ThreadPoolExecutor}拓展。添加了对于{@link HashExecutor}的支持。
 * 提供了排队功能，而排队功能（暂时）无法使用动态线程数量。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class SharedQueueExecutor extends ThreadPoolExecutor implements HashExecutor {

	/**
	 * 执行器名称
	 */
	private final String name;

	/**
	 * 主锁
	 */
	private final ReentrantLock lock = new ReentrantLock();

	/**
	 * 等待条件
	 */
	private final Condition notFull = lock.newCondition();

	/**
	 * 拒绝执行策略
	 * <p>
	 * 当任务无法提交到队列中时调用，例如：
	 * <ul>
	 *     <li>线程队列已满</li>
	 *     <li>执行器已关闭</li>
	 * </ul>
	 */
	private final RejectedHashExecutionHandler rejectedHandler;

	/**
	 * （阻塞调用者策略）是否阻塞调用者
	 */
	private final boolean blockingCaller;

	/**
	 * 待执行的任务队列
	 */
	private final BlockingDeque<Runnable> sharedQueue;

	/**
	 * 共享队列执行器
	 *
	 * @param name            执行器名称
	 * @param nThreads        启动线程
	 * @param rejectedHandler 拒绝执行策略
	 */
	public SharedQueueExecutor(String name, int nThreads, RejectedHashExecutionHandler rejectedHandler) {
		this(name, nThreads, new LinkedBlockingDeque<>(8), rejectedHandler);
	}

	private SharedQueueExecutor(String name, int nThreads, BlockingDeque<Runnable> sharedQueue, RejectedHashExecutionHandler rejectedHandler) {
		super(nThreads, nThreads, 0, TimeUnit.MILLISECONDS, sharedQueue, new InternalRejectedExecutionHandler());
		this.name = name;
		this.sharedQueue = sharedQueue;
		this.rejectedHandler = rejectedHandler;
		this.blockingCaller = rejectedHandler instanceof BlockingCallerHandler;
		init();
	}

	private void init() {
		for (int i = 0; i < getCorePoolSize(); i++) {
			execute(() -> {
			});
		}
	}

	private void reject(int hash, Runnable task, boolean last) {
		// 检测线程池是否已经关闭，如果线程池关闭，则直接调用拒绝策略
		if (!isShutdown() && blockingCaller) {
			try {
				lock.lockInterruptibly();
				try {
					while (last ? !sharedQueue.offerLast(task) : !sharedQueue.offerFirst(task)) {
						notFull.await();
						// 线程被唤醒后，先检查线程池是否关闭。
						if (isShutdown()) {
							rejectedHandler.rejectedExecution(hash, task, this);
							return;
						}
					}
				} finally {
					lock.unlock();
				}
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		} else {
			rejectedHandler.rejectedExecution(hash, task, this);
		}
	}

	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		super.beforeExecute(t, r);
		if (blockingCaller) {
			try {
				lock.lockInterruptibly();
				try {
					notFull.signal();
					System.out.println("notify signal");
				} finally {
					lock.unlock();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int size() {
		return sharedQueue.size();
	}

	@Override
	public void execute(int hash, Runnable task) {
		try {
			execute(task);
		} catch (InternalException e) {
			System.out.println("go reject");
			reject(hash, task, true);
		}
	}

	@Override
	public void executeNow(int hash, Runnable task) {
		try {
			execute(task);
		} catch (InternalException e) {
			reject(hash, task, false);
		}
	}

	@Override
	public Future<?> submit(int hash, Runnable task) {
		if (task == null) {
			throw new NullPointerException();
		}
		RunnableFuture<Void> future = newTaskFor(task, null);
		execute(hash, future);
		return future;
	}

	@Override
	public Future<?> submitNow(int hash, Runnable task) {
		if (task == null) {
			throw new NullPointerException();
		}
		RunnableFuture<Void> future = newTaskFor(task, null);
		executeNow(hash, future);
		return future;
	}

	@Override
	public <T> Future<T> submit(int hash, Callable<T> task) {
		if (task == null) {
			throw new NullPointerException();
		}
		RunnableFuture<T> future = newTaskFor(task);
		execute(hash, future);
		return future;
	}

	@Override
	public <T> Future<T> submitNow(int hash, Callable<T> task) {
		if (task == null) {
			throw new NullPointerException();
		}
		RunnableFuture<T> future = newTaskFor(task);
		executeNow(hash, future);
		return future;
	}

	@Override
	public void shutdown() {
		super.shutdown();
		if (blockingCaller) {
			try {
				lock.lockInterruptibly();
				try {
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
	 * 关联{@link RejectedExecutionHandler}和{@link RejectedHashExecutionHandler}
	 */
	private static final class InternalRejectedExecutionHandler implements RejectedExecutionHandler {

		@Override
		public void rejectedExecution(Runnable r, java.util.concurrent.ThreadPoolExecutor executor) {
			throw new InternalException();
		}
	}

	private static final class InternalException extends RuntimeException {

	}
}
