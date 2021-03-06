package com.keimons.nutshell.explorer;

import com.keimons.nutshell.explorer.support.DirectExplorer;
import com.keimons.nutshell.explorer.support.QueueExplorer;
import com.keimons.nutshell.explorer.support.SharedQueueExplorer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.Objects;

/**
 * 任务执行器
 * <p>
 * 系统允许定义最多128个任务执行器。每个执行器需要实现{@link ExplorerService}接口。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @see DirectExplorer 即时执行器（无队列）
 * @see SharedQueueExplorer 共享队列执行器
 * @see QueueExplorer 哈希队列执行器
 * @since 17
 **/
public class ExecutorManager {

	/**
	 * 默认任务提交策略
	 * <p>
	 * 使用这个策略，会绑定一个唯一的对象，并且，在这个对象中排队提交，当且仅当一个任务完成后，
	 * 下一个任务才会提交到任务执行器中。
	 *
	 * @see DirectExplorer 无队列任务执行器
	 */
	public static final int NONE_EXECUTOR_STRATEGY = 0;

	/**
	 * 默认的任务提交策略
	 *
	 * @see DirectExplorer 排队提交任务策略
	 */
	public static final int DEFAULT_EXECUTOR_STRATEGY = NONE_EXECUTOR_STRATEGY;

	/**
	 * 任务执行策略
	 *
	 * @see DirectExplorer 无队列任务执行器
	 * @see QueueExplorer 哈希队列任务执行器
	 * @see SharedQueueExplorer 共享队列任务执行器
	 */
	private static final ExplorerService[] strategies = new ExplorerService[128];

	static {
		// 无操作任务执行器
		registerExecutorStrategy(NONE_EXECUTOR_STRATEGY, new DirectExplorer());
	}

	/**
	 * 获取一个任务执行策略
	 *
	 * @param executorIndex 任务执行器
	 * @return 任务执行策略
	 */
	@SuppressWarnings("unchecked")
	public static <T extends ExplorerService> T getExecutorStrategy(
			@Range(from = 0, to = 127) int executorIndex) {
		return (T) strategies[executorIndex];
	}

	/**
	 * 使用任务执行策略，执行一个任务
	 *
	 * @param executorIndex 任务执行策略
	 * @param task          等待执行的任务
	 * @param fences        执行屏障
	 */
	public static void executeTask(int executorIndex, Runnable task, Object... fences) {
		strategies[executorIndex].execute(task, fences);
	}

	/**
	 * 注册一个任务执行器
	 *
	 * @param executorIndex 任务执行器
	 * @param strategy      任务执行策略
	 */
	public static synchronized void registerExecutorStrategy(
			@Range(from = 0, to = 127) int executorIndex,
			@NotNull ExplorerService strategy) {
		if (Objects.nonNull(strategies[executorIndex])) {
			throw new StrategyExistedException("executor");
		}
		strategies[executorIndex] = strategy;
	}
}
