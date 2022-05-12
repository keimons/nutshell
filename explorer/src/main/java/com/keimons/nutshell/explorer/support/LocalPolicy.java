package com.keimons.nutshell.explorer.support;

import com.keimons.nutshell.explorer.ExplorerService;
import com.keimons.nutshell.explorer.RejectedTrackExecutionHandler;

/**
 * 本地执行
 * <p>
 * 由调用者线程直接执行任务。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class LocalPolicy implements RejectedTrackExecutionHandler {

	@Override
	public void rejectedExecution(Object fence, Runnable task, ExplorerService executor) {
		task.run();
	}
}