package com.keimons.nutshell.disruptor;

/**
 * 轨道屏障
 * <p>
 * 当线程被拦截器拦截后，如果线程处于休眠/自旋等待时，影响吞吐量。大部分时候，期望针对于单Key是串行执行，而不相关的Key可以重排序执行，也就是越障执行。
 * 它其实很像单行公路上的交警，仅仅拦截某个型号的汽车，让其停靠在路边，而其它型号的汽车则可以提前通过。当收到放行指令后，所有被拦截的汽车优先于正在等待通行的汽车，依次通行。
 * 设计目的：
 * <ul>
 *     <li>保证对外表现的一致。</li>
 *     <li>它不是神丹妙药，但在力所能及的范围内避免死锁。</li>
 *     <li>降低多线程编码门槛和难度。</li>
 *     <li>它依然是适用于redis的，但是对于具有唯一ID的，例如：地块ID、公会ID等，有了更好的表现。</li>
 * </ul>
 * 通过对于仅拦截指定的Key，而不是全拦截，从而提升吞吐量。线程始终处于运行状态，如图：
 * <pre>
 *            +----------------------------------------+     +------------+
 * QueueA  -> | Key0 Key2 Key4 |      | Key2           | --> |  Thread A  |
 *            +----------------+ Key2 +----------------+     +------------+
 *                             |  +   |
 *            +----------------+ Key3 +----------------+     +------------+
 * QueueB  -> |           Key1 |      | Key1 Key3 Key5 | --> |  Thread B  |
 *            +----------------------------------------+     +------------+
 * </pre>
 * {@code Key2 + Key3}是一个共享任务，被两个队列共享，但期望整个任务最终只会被一个线程所执行。假定所有任务执行时长是一样的，任务执行：
 * <ul>
 *     <li>...</li>
 *     <li>
 *         第一时刻：{@code Thread A}处理{@code Key2}；{@code Thread B}处理{@code Key5}。
 *     </li>
 *     <li>
 *         第二时刻：{@code Thread A}处理完{@code Key2}后越障，处理{@code Key4}；{@code Thread B}处理{@code Key3}。
 *     </li>
 *     <li>
 *         第三时刻：{@code Thread A}遇到{@code Key2}存储任务并跳过，处理{@code Key0}；{@code Thread B}处理{@code Key1}。
 *     </li>
 *     <li>
 *         第四时刻：{@code Thread A}进入休眠/自旋；{@code Thread B}处理共享任务{@code Key2 + Key3}。
 *     </li>
 *     <li>
 *         第五时刻：{@code Thread A}跳过共享任务{@code Key2 + Key3}，处理存储的{@code Key2}；{@code Thread B}处理{@code Key1}。
 *     </li>
 *     <li>
 *         第六时刻：{@code Thread A}空闲；{@code Thread B}空闲。
 *     </li>
 *     <li>...</li>
 * </ul>
 * 与二维队列职责不同的是，二维队列解决任务交叉投递造成的死锁，而越障执行却能提高吞吐量，同时，还能保证任务的串行执行。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public interface TrackBarrier {

	/**
	 * 使用一个栅栏初始化轨道屏障
	 *
	 * @param fence 栅栏
	 */
	void init(Object fence);

	/**
	 * 使用两个栅栏初始化轨道屏障
	 *
	 * @param fence0 栅栏0
	 * @param fence1 栅栏1
	 */
	void init(Object fence0, Object fence1);

	/**
	 * 使用三个栅栏初始化轨道屏障
	 *
	 * @param fence0 栅栏0
	 * @param fence1 栅栏1
	 * @param fence2 栅栏2
	 */
	void init(Object fence0, Object fence1, Object fence2);

	/**
	 * 使用多个栅栏初始化轨道屏障
	 *
	 * @param fences 栅栏
	 */
	void init(Object... fences);

	/**
	 * 获取截取量
	 *
	 * @return 截取量
	 */
	int intercept();

	/**
	 * 获取所有hash目标值
	 *
	 * @return hash目标值
	 * @deprecated 过时的设计
	 */
	@Deprecated
	int[] hashes();

	boolean isTrack(long bits);

	/**
	 * 判定另一个轨道化对象能否在指定的轨道越过当前栅栏执行
	 *
	 * @param track   轨道
	 * @param barrier 执行屏障
	 * @return {@code true}可以越过当前栅栏，{@code false}不能越过当前栅栏
	 */
	boolean reorder(int track, TrackBarrier barrier);

	/**
	 * 释放屏障
	 */
	void release();
}
