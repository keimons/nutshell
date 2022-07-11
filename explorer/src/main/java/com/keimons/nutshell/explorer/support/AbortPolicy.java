package com.keimons.nutshell.explorer.support;

import com.keimons.nutshell.explorer.ExplorerService;
import com.keimons.nutshell.explorer.RejectedExplorerHandler;

import java.util.concurrent.RejectedExecutionException;

/**
 * 中止执行
 * <p>
 * 直接抛出异常交由调用者处理。这也是{@link ExplorerService}的默认拒绝策略。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
public class AbortPolicy implements RejectedExplorerHandler {

	@Override
	public void rejectedExecution(ExplorerService executor, Runnable task, Object... fences) {
		throw new RejectedExecutionException("Task " + task.toString() + " rejected from " + executor.getName());
	}
}
