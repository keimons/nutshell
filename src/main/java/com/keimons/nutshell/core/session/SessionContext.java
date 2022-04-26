package com.keimons.nutshell.core.session;

import com.keimons.nutshell.core.ApplicationContext;
import com.keimons.nutshell.core.Autolink;
import com.keimons.nutshell.core.nutshell.Protagonist;
import com.keimons.nutshell.core.sequence.Sequencer;

import java.util.HashMap;
import java.util.Map;

/**
 * Session上下文
 * <p>
 * 三级Context中的第二级
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class SessionContext {

	@Autolink
	ApplicationContext context;

	@Autolink
	Sequencer sequencer;

	private Map<Object, Protagonist> containers = new HashMap<>();

	public <T> T getBean(Object sessionId, Class<T> clazz) {
		return getBean(sessionId, sequencer.getIndex(clazz));
	}

	public <T> T getBean(Object sessionId, int index) {
		Protagonist mc = containers.get(sessionId);
		return mc.get(index);
	}
}
