package com.keimons.nutshell.disruptor;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.LockSupport;

/**
 * 拦截器
 * <p>
 * <p>
 * 拦截器的设计，是为了保证：当一个任务投递到多个队列并被多个线程消费时，最终只有一个线程能够执行这个任务。
 * 它采用的是反转{@link Semaphore}的设计。先到达的线程拦截并计数，当满足拦截量后，此时到达线程才能执行。
 * 如图：
 * <pre>
 *            +-----------------------------------+     +------------+
 * QueueA  -> | Task1 Task2 |       | Task5       | --> |  Thread A  |
 *            +-------------+       +-------------+     +------------+
 *                          | Task4 |
 *            +-------------+       +-------------+     +------------+
 * QueueB  -> |       Task3 |       | Task6 Task7 | --> |  Thread B  |
 *            +-----------------------------------+     +------------+
 * </pre>
 * {@code Task4}被两个队列共享，但期望{@code Task4}最终只会被一个线程所执行。假定所有任务执行时长是一样的，任务执行：
 * <ul>
 *     <li>...</li>
 *     <li>
 *         第一时刻：{@code Thread A}处理{@code Task5}；{@code Thread B}处理{@code Task7}。
 *     </li>
 *     <li>
 *         第二时刻：{@code Thread A}处理完{@code Task5}后被拦截，进入休眠（取决于实现）；{@code Thread B}处理{@code Task6}。
 *     </li>
 *     <li>
 *         第三时刻：{@code Thread A}继续休眠；{@code Thread B}处理{@code Task4}。
 *     </li>
 *     <li>
 *         第四时刻：唤醒{@code Thread A}，跳过{@code Task4}继续处理{@code Task2}；{@code Thread B}处理{@code Task3}。
 *     </li>
 *     <li>
 *         第五时刻：{@code Thread A}处理{@code Task1}，{@code Thread B}空闲。
 *     </li>
 *     <li>...</li>
 * </ul>
 * 它两个重要的概念：拦截量和全放行。只有满足拦截量后到达的线程才能执行任务，任务只被执行一次，执行完成后，所有被拦截的线程直接放行。
 * <p>
 * 设计期望可选实现：
 * <ul>
 *     <li>停顿方面：{@link LockSupport#park()}或{@link Thread#yield()}。</li>
 *     <li>提升吞吐量：停顿时，根据一些规则判断后续任务是否提前执行。例如：通过给定的{@code key}，后续无关{@code key}直接执行，相关{@code key}排队执行。</li>
 * </ul>
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public interface Interceptor {

	/**
	 * 初始化
	 *
	 * @param forbids 拦截量
	 */
	void init(int forbids);

	/**
	 * 尝试拦截当前线程
	 * <p>
	 * 对于拦截量以内的线程进行拦截，后续线程放行。
	 *
	 * @return {@code true}拦截成功，{@code false}拦截失败
	 */
	boolean intercept();

	/**
	 * 尝试拦截当前线程
	 * <p>
	 * 对于拦截量以内的线程进行拦截，后续线程放行。
	 *
	 * @return {@code true}拦截成功，{@code false}拦截失败
	 */
	boolean tryIntercept();

	/**
	 * 是否正在被拦截
	 *
	 * @return {@code true}已拦截，{@code false}全放行。
	 */
	boolean isIntercepted();

	/**
	 *
	 */
	void release();
}
