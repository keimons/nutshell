package com.keimons.nutshell.explorer.support;

import com.keimons.nutshell.explorer.ExplorerService;
import com.keimons.nutshell.explorer.RejectedExplorerHandler;

/**
 * 本地执行
 * <p>
 * 由调用者线程直接执行任务。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class LocalPolicy implements RejectedExplorerHandler {

	@Override
	public void rejectedExecution(ExplorerService executor, Runnable task, Object... fences) {
		task.run();
	}
}
