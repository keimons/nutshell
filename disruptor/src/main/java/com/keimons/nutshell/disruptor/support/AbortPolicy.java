package com.keimons.nutshell.disruptor.support;

import com.keimons.nutshell.disruptor.RejectedTrackExecutionHandler;
import com.keimons.nutshell.disruptor.TrackExecutor;

import java.util.concurrent.RejectedExecutionException;

/**
 * 中止执行
 * <p>
 * 直接抛出异常交由调用者处理。这也是{@link TrackExecutor}的默认拒绝策略。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class AbortPolicy implements RejectedTrackExecutionHandler {

	@Override
	public void rejectedExecution(Object fence, Runnable task, TrackExecutor executor) {
		throw new RejectedExecutionException("Task " + task.toString() + " rejected from " + executor.getName());
	}
}
