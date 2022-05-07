package com.keimons.nutshell.explorer.support;

import com.keimons.nutshell.explorer.RejectedTrackExecutionHandler;
import com.keimons.nutshell.explorer.Explorer;

import java.util.concurrent.RejectedExecutionException;

/**
 * 中止执行
 * <p>
 * 直接抛出异常交由调用者处理。这也是{@link Explorer}的默认拒绝策略。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class AbortPolicy implements RejectedTrackExecutionHandler {

	@Override
	public void rejectedExecution(Object fence, Runnable task, Explorer executor) {
		throw new RejectedExecutionException("Task " + task.toString() + " rejected from " + executor.getName());
	}
}
