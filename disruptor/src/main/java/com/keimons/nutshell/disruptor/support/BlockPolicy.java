package com.keimons.nutshell.disruptor.support;

import com.keimons.nutshell.disruptor.BlockingCallerHandler;
import com.keimons.nutshell.disruptor.TrackExecutor;

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
	public void rejectedExecution(Object fence, Runnable task, TrackExecutor executor) {
		throw new RejectedExecutionException("Task " + task.toString() + " rejected from " + executor.getName());
	}
}
