package com.keimons.nutshell.disruptor.internal;

/**
 * 事件工厂
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
public interface EventFactory<E> {

	E newInstance();
}
