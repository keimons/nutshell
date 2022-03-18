package com.keimons.nutshell.dispenser.support;

import com.keimons.nutshell.dispenser.HashExecutor;
import com.keimons.nutshell.dispenser.RejectedHashExecutionHandler;

import java.util.concurrent.RejectedExecutionException;

/**
 * 中止执行
 * <p>
 * 直接抛出异常交由调用者处理。这也是{@link HashExecutor}的默认拒绝策略。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class AbortPolicy implements RejectedHashExecutionHandler {

	@Override
	public void rejectedExecution(int hash, Runnable task, HashExecutor executor) {
		throw new RejectedExecutionException("Task " + task.toString() + " rejected from " + executor.getName());
	}
}
