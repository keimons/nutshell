package com.keimons.nutshell.dispenser;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

/**
 * 即时执行器
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
	public void shutdown() {

	}
}
