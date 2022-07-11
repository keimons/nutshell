package com.keimons.nutshell.core;

/**
 * 乐观同步器
 * <p>
 * 区别于锁，它仅仅用于状态的同步或状态变更的验证。乐观同步器通常由两部分组成：版本和状态。
 * 通常情况下，状态并不会直接暴露给外部使用，但是可以通过版本的变更，从而触发状态的变更。
 * 基于版本和状态的变化验证。乐观同步只有三个关键要素：
 * <ul>
 *     <li><b>读</b>，获取当前版本，用于判断状态在读和验证期间，是否发生了改变。</li>
 *     <li><b>写</b>，写入一个版本，标识当前的状态已经发生了改变，需要重新读取状态。</li>
 *     <li><b>验证</b>，验证一个版本是否已经过期。</li>
 * </ul>
 * 乐观同步器，它用于验证在读取期间，状态是否发生改变：
 * <pre>{@code
 *     OptimisticSynchronizer sync = ...;
 *     int stamp = sync.acquireRead();
 *     // sync.acquireWrite(); // 修改状态
 *     // do something
 *     sync.validate(stamp);
 * }</pre>
 * 乐观同步器是一个无锁化的设计，它适用于实现多生产者-单消费者的高性能的事件总线，提供了同步机制，
 * 以保证尽可能的在必要的时候，才会尝试唤醒休眠的线程。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
public interface OptimisticSynchronizer {

	/**
	 * 获取一个状态
	 * <p>
	 * 返回的状态可以应用于稍后{@link #validate(int)}校验，以判断期间是否发生过改变。
	 *
	 * @return 当前最新的状态
	 */
	int acquireRead();

	/**
	 * 状态变更
	 * <p>
	 * 变更一次当前的状态，如果当前正在发生读取，在稍后的验证时，可以捕获到这个状态的变更。
	 */
	void acquireWrite();

	/**
	 * 验证一个状态
	 * <p>
	 * 验证{@link #acquireRead()}获取的状态，用于判断期间是否发生{@link #acquireWrite()}，从而导致状态过期。
	 * 注意，这个方法没有返回值！乐观同步器不会单独的存在，通常会和其它组件联合工作的。
	 *
	 * @param stamp 即将验证的状态
	 */
	void validate(int stamp);
}
