package com.keimons.nutshell.explorer.test.demo;

/**
 * 帮派任务执行器
 * <p>
 * 无锁化设计。被投递到执行器的任务，会按照{@link Union#getUnionId()}进行任务派发。
 * 保证了来自同一个帮派的任务，总是由同一个线程处理，从而防止帮派自身并发。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
public class UnionExecutor {

	private static final int DEFAULT_N_THREAD = 20;

	private static final Worker[] WORKERS = new Worker[DEFAULT_N_THREAD];

	static {
		// 略 init(WORKERS);
	}

	/**
	 * 执行帮派相关任务
	 *
	 * @param task  任务
	 * @param union 帮派
	 */
	public void execute(Runnable task, Union union) {
		int index = union.getUnionId() % DEFAULT_N_THREAD;
		WORKERS[index].offer(task);
	}
}
