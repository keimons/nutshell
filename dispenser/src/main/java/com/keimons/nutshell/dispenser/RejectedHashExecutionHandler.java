package com.keimons.nutshell.dispenser;

import com.keimons.nutshell.dispenser.support.AbortPolicy;
import com.keimons.nutshell.dispenser.support.BlockPolicy;
import com.keimons.nutshell.dispenser.support.LocalPolicy;

import java.util.concurrent.RejectedExecutionException;

/**
 * 无法由{@link HashExecutor}执行的任务的处理程序
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public interface RejectedHashExecutionHandler {

	/**
	 * 拒绝执行
	 * <p>
	 * 当{@link HashExecutor}拒绝执行此任务时调用。拒绝执行的原因可能是：
	 * <ul>
	 *     <li>没有线程执行此任务。</li>
	 *     <li>队列已满，任务不能加入到队列中。</li>
	 *     <li>{@link HashExecutor}已经关闭。</li>
	 * </ul>
	 * 当任务被拒绝执行时，提供以下解决方案：
	 * <ul>
	 *     <li>{@link AbortPolicy}中止策略，抛出异常。</li>
	 *     <li>{@link BlockPolicy}阻塞线程。</li>
	 *     <li>{@link LocalPolicy}本地执行。</li>
	 *     <li>自定义策略。</li>
	 * </ul>
	 * 哈希线程池设计时在一定意义上规避了一些并发问题，当被拒绝执行的任务采用本地执行，
	 * 或自定义策略时，应注意这将造成额外的并发问题。这是危险的。
	 *
	 * @param hash
	 * @param task     请求执行的任务
	 * @param executor 任务的执行者
	 * @throws RejectedExecutionException 拒绝执行异常
	 */
	void rejectedExecution(int hash, Runnable task, HashExecutor executor);
}
