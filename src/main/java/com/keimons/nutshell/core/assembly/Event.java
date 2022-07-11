package com.keimons.nutshell.core.assembly;

/**
 * Event
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
@FunctionalInterface
public interface Event {

	/**
	 * 事件触发
	 *
	 * @param params 事件参数
	 * @throws Throwable 事件执行异常
	 */
	void onEvent(Object... params) throws Throwable;
}
