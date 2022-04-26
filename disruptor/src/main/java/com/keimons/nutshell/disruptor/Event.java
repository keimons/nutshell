package com.keimons.nutshell.disruptor;

/**
 * 事件
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class Event {

	protected volatile State state = State.FREE;

	protected volatile Event event;

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	@SuppressWarnings("unchecked")
	public <E extends Event> E getEvent() {
		return (E) event;
	}

	public void setEvent(Event event) {
		this.event = event;
	}

	enum State {
		FREE, WORK, READY
	}
}
