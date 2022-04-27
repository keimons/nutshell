package com.keimons.nutshell.disruptor.test;

import com.keimons.nutshell.disruptor.internal.BitsTrackEventBus;
import com.keimons.nutshell.disruptor.internal.EventBus;
import org.junit.jupiter.api.Test;

/**
 * {@link EventBus}消息总线测试
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class EventBusTest {

	@Test
	public void test() {
		EventBus<Object> eventBus = new BitsTrackEventBus<>(Object::new, 1);
		for (int i = 0; i < 3; i++) {
			eventBus.borrowEvent();
		}
		for (int i = 0; i < 3; i++) {
			eventBus.returnEvent(new Object());
		}
	}
}
