package com.keimons.nutshell.dispenser.support;

import com.keimons.nutshell.dispenser.HashExecutor;
import com.keimons.nutshell.dispenser.RejectedHashExecutionHandler;

/**
 * 本地执行
 * <p>
 * 由调用者线程直接执行任务。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class LocalPolicy implements RejectedHashExecutionHandler {

	@Override
	public void rejectedExecution(int hash, Runnable task, HashExecutor executor) {
		task.run();
	}
}
