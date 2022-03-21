package com.keimons.nutshell.dispenser.support;

import com.keimons.nutshell.dispenser.AbstractHashExecutor;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

/**
 * 即时执行器
 * <p>
 * 即时执行器是哈希执行器的简单实现，它并不是异步执行，而是直接使用提交任务的线程立即执行任务，
 * 它并不包含任何调度、队列和线程。如果任务阻塞，调用者线程同样会发生阻塞。
 * <p>
 * 由这个执行器执行的任务，应该是CPU密集型运算，并且是无阻塞或IO等。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class DirectExecutor extends AbstractHashExecutor {

	private static final String NAME = "DIRECT";

	public DirectExecutor() {
		this(NAME);
	}

	public DirectExecutor(String name) {
		super(name, 0, DefaultRejectedHandler);
	}

	@Override
	public void execute(int hash, Runnable task) {
		task.run();
	}

	@Override
	public void executeNow(int hash, Runnable task) {
		task.run();
	}

	@Override
	public Future<?> submit(int hash, Runnable task) {
		RunnableFuture<?> future = new FutureTask<>(task, null);
		future.run();
		return future;
	}

	@Override
	public Future<?> submitNow(int hash, Runnable task) {
		return submit(hash, task);
	}

	@Override
	public <T> Future<T> submit(int hash, Callable<T> task) {
		FutureTask<T> future = new FutureTask<>(task);
		future.run();
		return future;
	}

	@Override
	public <T> Future<T> submitNow(int hash, Callable<T> task) {
		return submit(hash, task);
	}

	@Override
	public boolean isShutdown() {
		return running;
	}

	@Override
	public void shutdown() {
		// do nothing
	}
}
