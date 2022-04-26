package com.keimons.nutshell.disruptor.support;

import com.keimons.nutshell.disruptor.RejectedTrackExecutionHandler;
import com.keimons.nutshell.disruptor.TrackBarrier;
import com.keimons.nutshell.disruptor.TrackExecutor;

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
	public void rejectedExecution(TrackBarrier barrier, Runnable task, TrackExecutor executor) {
		task.run();
	}
}
