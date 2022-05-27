package com.keimons.nutshell.explorer;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * 消费者任务
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
public class ConsumerTask<V> implements ConsumerFuture<V> {

	/**
	 * 任务在未来的某一刻执行的任务
	 */
	FutureTask<V> task = new FutureTask<>(this::take);

	/**
	 * 消费者
	 */
	Consumer<V> consumer;

	/**
	 * 执行结果
	 */
	V result;

	public ConsumerTask(Consumer<V> consumer) {
		this.consumer = consumer;
	}

	private V take() {
		return result;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return task.cancel(mayInterruptIfRunning);
	}

	@Override
	public boolean isCancelled() {
		return task.isCancelled();
	}

	@Override
	public boolean isDone() {
		return task.isDone();
	}

	@Override
	public V get() throws InterruptedException, ExecutionException {
		return task.get();
	}

	@Override
	public V get(long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return task.get(timeout, unit);
	}

	@Override
	public void accept(V v) {
		result = v;
		task.run();
		consumer.accept(v);
	}
}
