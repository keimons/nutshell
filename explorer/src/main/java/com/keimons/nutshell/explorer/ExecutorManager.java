package com.keimons.nutshell.explorer;

import com.keimons.nutshell.explorer.support.DirectExecutor;
import com.keimons.nutshell.explorer.support.QueueExplorer;
import com.keimons.nutshell.explorer.support.SharedQueueExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.Objects;

/**
 * 任务执行器
 * <p>
 * 系统允许定义最多128个任务执行器。每个执行器需要实现{@link Explorer}接口。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @see DirectExecutor 即时执行器（无队列）
 * @see SharedQueueExecutor 共享队列执行器
 * @see QueueExplorer 哈希队列执行器
 * @since 11
 **/
public class ExecutorManager {

	/**
	 * 默认任务提交策略
	 * <p>
	 * 使用这个策略，会绑定一个唯一的对象，并且，在这个对象中排队提交，当且仅当一个任务完成后，
	 * 下一个任务才会提交到任务执行器中。
	 *
	 * @see DirectExecutor 无队列任务执行器
	 */
	public static final int NONE_EXECUTOR_STRATEGY = 0;

	/**
	 * 默认的任务提交策略
	 *
	 * @see DirectExecutor 排队提交任务策略
	 */
	public static final int DEFAULT_EXECUTOR_STRATEGY = NONE_EXECUTOR_STRATEGY;

	/**
	 * 任务执行策略
	 *
	 * @see DirectExecutor 无队列任务执行器
	 * @see QueueExplorer 哈希队列任务执行器
	 * @see SharedQueueExecutor 共享队列任务执行器
	 */
	private static final Explorer[] strategies = new Explorer[128];

	static {
		// 无操作任务执行器
		registerExecutorStrategy(NONE_EXECUTOR_STRATEGY, new DirectExecutor());
	}

	/**
	 * 获取一个任务执行策略
	 *
	 * @param executorIndex 任务执行器
	 * @return 任务执行策略
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Explorer> T getExecutorStrategy(
			@Range(from = 0, to = 127) int executorIndex) {
		return (T) strategies[executorIndex];
	}

	/**
	 * 使用任务执行策略，执行一个任务
	 *
	 * @param executorIndex 任务执行策略
	 * @param barrier       执行屏障
	 * @param task          等待执行的任务
	 */
	public static void executeTask(int executorIndex, TrackBarrier barrier, Runnable task) {
		strategies[executorIndex].execute(task, barrier);
	}

	/**
	 * 注册一个任务执行器
	 *
	 * @param executorIndex 任务执行器
	 * @param strategy      任务执行策略
	 */
	public static synchronized void registerExecutorStrategy(
			@Range(from = 0, to = 127) int executorIndex,
			@NotNull Explorer strategy) {
		if (Objects.nonNull(strategies[executorIndex])) {
			throw new StrategyExistedException("executor");
		}
		strategies[executorIndex] = strategy;
	}
}
