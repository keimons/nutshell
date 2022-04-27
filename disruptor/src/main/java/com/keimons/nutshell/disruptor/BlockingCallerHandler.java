package com.keimons.nutshell.disruptor;

/**
 * 阻塞调用线程
 * <p>
 * 尝试阻塞调用者线程，但是仍然有可能发生阻塞失败的情况。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public interface BlockingCallerHandler extends RejectedTrackExecutionHandler {

	/**
	 * 阻塞失败
	 * <p>
	 * 阻塞调用者线程失败时调用，发生如下情况：
	 * <ul>
	 *     <li>线程池已关闭</li>
	 * </ul>
	 *  @param fence  执行屏障
	 *
	 * @param task     执行的任务
	 * @param executor 执行线程
	 */
	@Override
	void rejectedExecution(Object fence, Runnable task, TrackExecutor executor);
}