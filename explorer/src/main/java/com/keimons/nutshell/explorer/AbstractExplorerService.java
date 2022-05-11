package com.keimons.nutshell.explorer;

import com.keimons.nutshell.explorer.support.AbortPolicy;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * 任务执行器的抽象实现
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public abstract class AbstractExplorerService implements ExplorerService {

	protected static final RejectedTrackExecutionHandler DefaultRejectedHandler = new AbortPolicy();

	/**
	 * 策略名称
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
	protected ThreadFactory threadFactory = Executors.defaultThreadFactory();
	protected volatile boolean running = true;

	/**
	 * 哈希任务执行器
	 *
	 * @param name            执行器名称
	 * @param nThreads        线程数量
	 * @param rejectedHandler 被拒绝执行任务的处理句柄
	 */
	public AbstractExplorerService(String name, int nThreads, RejectedTrackExecutionHandler rejectedHandler) {
		this.name = name;
		this.nThreads = nThreads;
		this.rejectedHandler = rejectedHandler;
		this.blockingCaller = rejectedHandler instanceof BlockingCallerHandler;
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
