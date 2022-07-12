package com.keimons.nutshell.explorer;

/**
 * CommitterStrategy
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
public interface CommitterStrategy {

	Object DEFAULT = new Object();

	/**
	 * 提交一个任务
	 *
	 * @param key              提交者的唯一表示
	 * @param executorStrategy 任务执行策略
	 * @param task             等待执行的任务
	 */
	void commit(Object key, int executorStrategy, Runnable task, Object... fences);

	/**
	 * 刷新
	 * <p>
	 * 检测并清理已经长时间不用的任务提交者。
	 *
	 * @see LinkedCommitterPolicy 清理已经过期的Committer
	 */
	void refresh();
}
