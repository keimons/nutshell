package com.keimons.nutshell.explorer;

/**
 * 阻塞调用线程
 * <p>
 * 尝试阻塞调用者线程，但是仍然有可能发生阻塞失败的情况。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
public interface BlockingCallerHandler extends RejectedExplorerHandler {

	/**
	 * 阻塞失败
	 * <p>
	 * 阻塞调用者线程失败时调用，发生如下情况：
	 * <ul>
	 *     <li>线程池已关闭</li>
	 * </ul>
	 *
	 * @param explorer 执行线程
	 * @param task     执行的任务
	 * @param fences   执行屏障
	 */
	@Override
	void rejectedExecution(ExplorerService explorer, Runnable task, Object... fences);
}
