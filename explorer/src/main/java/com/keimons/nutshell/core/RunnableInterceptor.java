package com.keimons.nutshell.core;

import com.keimons.nutshell.explorer.Interceptor;
import com.keimons.nutshell.explorer.support.Threadripper;

/**
 * 可运行的拦截器
 * <p>
 * 可执行的拦截器中包含：
 * <ol>
 *     <li>任务唯一序列（可能没有）；</li>
 *     <li>任务；</li>
 *     <li>任务屏障（可能有多个）；</li>
 *     <li>任务轨道（可能有多个）；</li>
 *     <li>拦截器信息。</li>
 * </ol>
 * 它以不同的身份被多个线程持有，节点中拦截器的释放变得更加复杂且难以复用，这使得节点也变成了一次性的消耗品。
 * 在多线程环境下，它有可能会被多个{@link Threadripper.Walker}持有，持有的形式包括：
 * <ul>
 *     <li>执行屏障，此时仅作为屏障，当拦截器释放时，屏障移除。</li>
 *     <li>缓存节点，当节点无法重排序到屏障之前时，将节点缓存，等待屏障释放后才能开始处理此节点。</li>
 *     <li>执行任务，此任务由最后一个碰到它的线程执行。</li>
 * </ul>
 * 同一个节点同时只会以一种形式被一个线程所持有，这三种状态是相互冲突的。
 * <p>
 * 同时，节点可以判断一个任务是否能由此线程处理，只有线程轨道和任务轨道重合时，节点才能由此线程处理，否则，忽略这个节点。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
public interface RunnableInterceptor extends Runnable, Interceptor {

	/**
	 * 设置任务唯一序列
	 * <p>
	 * 每一个任务在加入队列时，会给任务分配一个唯一序列。任务移除时，根据唯一序列移除任务。
	 * <p>
	 * 这不是必须的，例如：任务只有一个屏障，那么节点的持有是完全可预测的，此值将被忽略以节省内存。
	 *
	 * @param sequence 任务唯一序列
	 */
	void setSequence(long sequence);

	/**
	 * 获取任务唯一序列
	 *
	 * @return 任唯一序列
	 */
	long getSequence();

	/**
	 * 返回任务屏障的数量
	 * <p>
	 * Explorer的任务执行时，需要一个任务屏障，这个方法返回任务屏障数量。
	 *
	 * @return 任务屏障的数量
	 */
	int size();

	/**
	 * 唤醒线程
	 * <p>
	 * 唤醒线程的时机：
	 * <ul>
	 *     <li>任务发布，当任务发布到总线后，如果处理此任务的线程（们）处于休眠状态，则唤醒此线程（们）。</li>
	 *     <li>恢复屏障，当缓存节点恢复执行并且节点拦截成功时，唤醒处理此任务的其它线程。</li>
	 * </ul>
	 * 唤醒线程的实现是向该线程发放一个许可，多次/重复唤醒仅发放一个许可，当线程被唤醒后，许可被消耗。
	 * 线程多次被唤醒，仅仅会造成一些性能上的浪费，并不会造成运行时的问题。
	 */
	void weakUp();

	/**
	 * 返回此节点是否属于这个轨道
	 * <p>
	 * 任务附加的屏障同时决定了任务由哪个线程处理。这个方法用于判断某一个线程能否处理这个节点。
	 *
	 * @param track 轨道
	 * @return {@code true}线程处理此节点，{@code false}线程忽略此节点。
	 */
	boolean isTrack(int track);

	/**
	 * 返回节点是否只由一个线程处理
	 * <p>
	 * 尽管可能线程拥有多个屏障，如果多个屏障最终哈希到一个线程，那么也将由一个线程处理。
	 *
	 * @return {@code true}单线程任务，{@code false}多线程任务。
	 */
	boolean isAloneTrack();

	/**
	 * 返回其它节点是否能越过此节点（屏障）重排序执行
	 * <p>
	 * 如果任务屏障完全不同，则可以重排序执行，这对最终的结果不会产生影响。
	 *
	 * @param other 尝试越过此节点的其它节点
	 * @return {@code true}允许越过当前节点重排序运行，{@code false}禁止越过当前节点重排序运行。
	 */
	boolean isReorder(Threadripper.Node other);
}
