package com.keimons.nutshell.explorer.support;

import com.keimons.nutshell.explorer.AbstractExplorerService;
import com.keimons.nutshell.explorer.ConsumerFuture;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.*;

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
 * @since 17
 */
public class DirectExplorer extends AbstractExplorerService {

	private static final String NAME = "DirectExplorer";

	public DirectExplorer() {
		this(NAME);
	}

	public DirectExplorer(String name) {
		super(name, 0, DefaultRejectedHandler, Executors.defaultThreadFactory());
	}

	@Override
	public void execute(Runnable task, Object fence) {
		task.run();
	}

	@Override
	public void executeNow(Runnable task, Object fence) {
		task.run();
	}

	@Override
	public Future<?> submit(Runnable task, Object fence) {
		RunnableFuture<?> future = new FutureTask<>(task, null);
		future.run();
		return future;
	}

	@Override
	public Future<?> submitNow(Runnable task, Object fence) {
		return submit(task, fence);
	}

	@Override
	public <T> Future<T> submit(Callable<T> task, Object fence) {
		FutureTask<T> future = new FutureTask<>(task);
		future.run();
		return future;
	}

	@Override
	public <T> Future<T> submitNow(Callable<T> task, Object fence) {
		return submit(task, fence);
	}

	@Override
	public boolean isShutdown() {
		return running;
	}

	@Override
	public void close() {
		// do nothing
	}

	@Override
	public void close(RunnableFuture<?> onClose) {
		if (onClose != null) {
			onClose.run();
		}
	}

	@Override
	public void shutdown(ConsumerFuture<List<Runnable>> runnable) {
		// do nothing
	}

	public static class FinishFuture implements Future<Object> {

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return false;
		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public boolean isDone() {
			return true;
		}

		@Override
		public Object get() throws InterruptedException, ExecutionException {
			return null;
		}

		@Override
		public Object get(long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			return null;
		}
	}
}
