package com.keimons.nutshell.core;

/**
 * 乐观同步
 * <p>
 * 区别于锁，它仅仅用于状态的同步。在内部封装了一个版本的概念，基于版本变化的验证。提供三种模式：
 * <ul>
 *     <li><b>读</b>，获取当前版本，用于判断状态在读和验证期间，是否发生了改变。</li>
 *     <li><b>写</b>，写入一个版本，标识当前的状态已经发生了改变，需要重新读取状态。</li>
 *     <li><b>验证</b>，验证一个版本是否已经过期。</li>
 * </ul>
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
public interface OptimisticSync {

	/**
	 * 读
	 *
	 * @return 获取一个戳
	 */
	int acquireRead();

	/**
	 * 写
	 */
	void acquireWrite();

	/**
	 * 验证一个状态
	 *
	 * @param stamp 即将验证的状态
	 * @return 验证结果
	 */
	boolean validate(int stamp);
}
