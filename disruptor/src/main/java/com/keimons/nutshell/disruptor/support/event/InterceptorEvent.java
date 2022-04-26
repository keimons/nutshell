package com.keimons.nutshell.disruptor.support.event;

import com.keimons.nutshell.disruptor.Event;

/**
 * 默认事件实现
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class InterceptorEvent extends Event {

	Node nodes[];

	InterceptorEvent() {
		nodes = new Node[16];
	}

	private static class Node {

		Object[] objects = new Object[4];
	}
}
