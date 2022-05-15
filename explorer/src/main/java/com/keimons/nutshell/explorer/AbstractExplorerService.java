package com.keimons.nutshell.explorer;

import com.keimons.nutshell.explorer.support.AbortPolicy;

import java.util.concurrent.ThreadFactory;

/**
 * 任务执行器的抽象实现
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public abstract class AbstractExplorerService implements ExplorerService {

	/**
	 * 默认被拒绝任务的处理策略
	 * <p>
	 * 当消息队列已满时，如果继续向队列中写入任务，则调用执行此异常。
	 */
	public static final RejectedTrackExecutionHandler DefaultRejectedHandler = new AbortPolicy();

	/**
	 * 线程池名称
	 */
	protected final String name;

	/**
	 * 线程数量
	 */
	protected final int nThreads;

	/**
	 * 被拒绝执行的任务处理句柄
	 */
	protected final RejectedTrackExecutionHandler rejectedHandler;

	/**
	 * 是否阻塞调用者线程
	 * <p>
	 * 当队列没有足够空间时，是否阻塞调用者线程。
	 */
	protected final boolean blockingCaller;

	/**
	 * 线程工厂
	 */
	protected final ThreadFactory threadFactory;

	/**
	 * 线程池是否运行中
	 */
	protected volatile boolean running = true;

	/**
	 * 哈希任务执行器
	 *
	 * @param name            执行器名称
	 * @param nThreads        线程数量
	 * @param rejectedHandler 被拒绝执行任务的处理句柄
	 * @param threadFactory   线程工厂
	 */
	public AbstractExplorerService(String name, int nThreads, RejectedTrackExecutionHandler rejectedHandler, ThreadFactory threadFactory) {
		this.name = name;
		this.nThreads = nThreads;
		this.rejectedHandler = rejectedHandler;
		this.blockingCaller = rejectedHandler instanceof BlockingCallerHandler;
		this.threadFactory = threadFactory;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int size() {
		return nThreads;
	}
}
