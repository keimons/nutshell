package com.keimons.nutshell.disruptor.internal;

import jdk.internal.vm.annotation.Contended;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * 事件总线
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
public class BitsTrackEventBus<T> implements EventBus<T> {

	private static final VarHandle AA = MethodHandles.arrayElementVarHandle(Object[].class);
	final int mark;
	/**
	 * 缓存系统
	 */
	@Contended
	final T[] _buffer;
	/**
	 * 缓冲区
	 * <p>
	 * 移除数组两端的填充：
	 * <ul>
	 *     <li>Unsafe操作和直接调用差距很小。</li>
	 *     <li>带来了额外的计算量。</li>
	 *     <li>仅在两段数据操作时，造成影响。</li>
	 * </ul>
	 */
	@Contended
	final T[] buffer;
	private final int capacity;
	@Contended
	volatile long readerIndex;
	@Contended
	volatile long writerIndex;

	@SuppressWarnings("unchecked")
	public BitsTrackEventBus(EventFactory<T> factory, int capacity) {
		this.capacity = capacity;
		this.mark = capacity - 1;
		this._buffer = (T[]) new Object[capacity * 2];
		this.buffer = (T[]) new Object[capacity];
		fill(factory);
	}

	private void fill(EventFactory<T> factory) {
		for (int i = 0, length = capacity * 2; i < length; i++) {
			_buffer[i] = factory.newInstance();
		}
	}

	@Override
	public T borrowEvent() {
		for (; ; ) {
			long readerIndex = this.readerIndex;
			int offset = (int) (readerIndex & mark);
			T event = _buffer[offset];
			if (AA.compareAndSet(_buffer, offset, event, null)) {
				this.readerIndex = readerIndex + 1;
				return event;
			}
		}
	}

	@Override
	public long getWriterIndex() {
		return writerIndex;
	}

	@Override
	public void publishEvent(T event) {
		while (true) {
			long index = this.writerIndex;
			int offset = (int) (index & mark);
			if (AA.compareAndSet(buffer, offset, null, event)) {
				this.writerIndex = index + 1;
				return;
			} else {
				Thread.yield();
			}
		}
	}

	@Override
	public T getEvent(long index) {
		int offset = (int) (index & mark);
		return buffer[offset];
	}

	@Override
	public void returnEvent(long index) {
		int offset = (int) (index & mark);
		buffer[offset] = null;
	}
}
