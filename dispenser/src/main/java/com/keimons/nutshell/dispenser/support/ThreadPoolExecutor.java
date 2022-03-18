package com.keimons.nutshell.dispenser.support;

import com.keimons.nutshell.dispenser.BlockingCallerHandler;
import com.keimons.nutshell.dispenser.HashExecutor;
import com.keimons.nutshell.dispenser.RejectedHashExecutionHandler;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ThreadPoolExecutor
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class ThreadPoolExecutor extends java.util.concurrent.ThreadPoolExecutor implements HashExecutor {

	/**
	 * Main lock guarding all access
	 */
	final ReentrantLock lock = new ReentrantLock();

	/**
	 * Condition for waiting takes
	 */
	private final Condition notEmpty = lock.newCondition();

	/**
	 * Condition for waiting puts
	 */
	private final Condition notFull = lock.newCondition();

	RejectedHashExecutionHandler rejectedHandler;

	private LinkedBlockingDeque<Runnable> sharedQueue;

	public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, @NotNull TimeUnit unit, @NotNull BlockingQueue<Runnable> workQueue) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
	}

	public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, @NotNull TimeUnit unit, @NotNull BlockingQueue<Runnable> workQueue, @NotNull ThreadFactory threadFactory) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
	}

	public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, @NotNull TimeUnit unit, @NotNull BlockingQueue<Runnable> workQueue, @NotNull RejectedExecutionHandler handler) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
	}

	public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, @NotNull TimeUnit unit, @NotNull BlockingQueue<Runnable> workQueue, @NotNull ThreadFactory threadFactory, @NotNull RejectedExecutionHandler handler) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
	}

	private void reject(int hash, Runnable task) {
		// 检测线程池是否已经关闭，如果线程池关闭，则直接调用拒绝策略
		if (isShutdown()) {
			rejectedHandler.rejectedExecution(hash, task, this);
		}
		if (rejectedHandler instanceof BlockingCallerHandler) {
			try {
				lock.lockInterruptibly();
				try {
					while (!sharedQueue.offerLast(task)) {
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

	@Override
	public String getName() {
		return null;
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public void execute(int hash, Runnable task) {
		try {
			execute(task);
		} catch (Exception e) {

		}
	}

	@Override
	public void executeNow(int hash, Runnable task) {
		HashExecutor.super.executeNow(hash, task);
	}

	@Override
	public Future<?> submit(int hash, Runnable task) {
		return null;
	}

	@Override
	public Future<?> submitNow(int hash, Runnable task) {
		return HashExecutor.super.submitNow(hash, task);
	}

	@Override
	public <T> Future<T> submit(int hash, Callable<T> task) {
		return null;
	}

	@Override
	public <T> Future<T> submitNow(int hash, Callable<T> task) {
		return HashExecutor.super.submitNow(hash, task);
	}

	@Override
	public void shutdown() {
		super.shutdown();
		notFull.signalAll();
	}
}
