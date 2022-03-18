package com.keimons.nutshell.dispenser.support;

import com.keimons.nutshell.dispenser.BlockingCallerHandler;
import com.keimons.nutshell.dispenser.HashExecutor;

import java.util.concurrent.RejectedExecutionException;

/**
 * 阻塞线程
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class BlockPolicy implements BlockingCallerHandler {

	@Override
	public void rejectedExecution(int hash, Runnable task, HashExecutor executor) {
		throw new RejectedExecutionException("Task " + task.toString() + " rejected from " + executor.getName());
	}
}
