package com.keimons.nutshell.explorer.internal;

import jdk.internal.vm.annotation.ForceInline;

/**
 * 事件总线
 * <p>
 * 这是一个定制化的事件总线，事件总线维护一个对象数组和一个{@link #writerIndex()}写入位置。
 * 事件总线带有缓存功能，使用方法：
 * <ul>
 *     <li>在事件总线中获取事件对象。</li>
 *     <li>事件内容填入事件对象中。</li>
 *     <li>发布事件到事件总线。</li>
 *     <li>多线程读取事件总线中的事件，并判断是否为关注事件。</li>
 *     <li>归还事件对象。</li>
 * </ul>
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
public interface EventBus<T> {

	/**
	 * 获取写入位置
	 *
	 * @return 写入位置
	 */
	@ForceInline
	long writerIndex();

	/**
	 * 发布一个事件
	 *
	 * @param event 事件
	 * @return {@code true}发布成功，{@code false}发布失败
	 */
	boolean publishEvent(T event);

	/**
	 * 根据位置索引获取一个事件
	 *
	 * @param index 位置索引
	 * @return 该位置所存储的事件
	 */
	T getEvent(long index);

	/**
	 * 完成事件
	 *
	 * @param index 事件索引
	 */
	void finishEvent(long index);

	/**
	 * 测试一个标志位是否已结束
	 *
	 * @param writerIndex 下标
	 * @return 是否已结束
	 */
	boolean testWriterIndex(long writerIndex);

	/**
	 * 关闭环形缓冲区
	 */
	void shutdown();
}
