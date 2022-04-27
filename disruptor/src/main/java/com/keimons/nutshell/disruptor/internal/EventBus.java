package com.keimons.nutshell.disruptor.internal;

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

	long writerIndex();

	/**
	 * 借用事件对象
	 * <p>
	 * 如果要在{@link EventBus}中发布事件，需要先拿到事件对象，然后再发布事件。
	 *
	 * @return 事件对象
	 */
	T borrowEvent();

	void publishEvent(T event);

	/**
	 * 完成事件
	 *
	 * @param index 事件索引
	 */
	void finishEvent(long index);

	T getEvent(long index);

	/**
	 * 归还事件对象
	 *
	 * @param event 对象位置
	 */
	void returnEvent(T event);
}
