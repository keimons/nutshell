package com.keimons.nutshell.disruptor.internal;

import com.keimons.nutshell.disruptor.Debug;
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

	final EventFactory<T> factory;

	final int capacity;

	final int writerMark;

	final int readerMark;

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

	@Contended
	volatile long writerIndex;

	/**
	 * 缓存系统
	 */
	@Contended
	final T[] _buffer;

	@Contended
	volatile long _readerIndex;

	@Contended
	volatile long _writerIndex;

	@SuppressWarnings("unchecked")
	public BitsTrackEventBus(EventFactory<T> factory, int capacity) {
		this.factory = factory;
		this.capacity = capacity;
		this.writerMark = capacity - 1;
		this.readerMark = (capacity << 1) - 1;
		this._buffer = (T[]) new Object[capacity << 1];
		this.buffer = (T[]) new Object[capacity];
		fill();
	}

	private void fill() {
		for (int i = 0, length = capacity << 1; i < length; i++) {
			_buffer[i] = factory.newInstance();
		}
	}

	@Override
	public T borrowEvent() {
		for (; ; ) {
			long readerIndex = this._readerIndex;
			int offset = (int) (readerIndex & readerMark);
			T event = _buffer[offset];
			if (AA.compareAndSet(_buffer, offset, event, null)) {
				if (event == null) {
					// 缓冲区中没有消息了
					Debug.warn("队列已空");
					return factory.newInstance();
				}
				this._readerIndex = readerIndex + 1;
				return event;
			}
		}
	}

	@Override
	public long writerIndex() {
		return writerIndex;
	}

	@Override
	public void publishEvent(T event) {
		while (true) {
			long index = this.writerIndex;
			int offset = (int) (index & writerMark);
			if (AA.compareAndSet(buffer, offset, null, event)) {
				this.writerIndex = index + 1;
				return;
			} else {
				Thread.yield();
			}
		}
	}

	@Override
	public void finishEvent(long index) {
		int offset = (int) (index & writerMark);
		buffer[offset] = null;
	}

	@Override
	public T getEvent(long index) {
		int offset = (int) (index & writerMark);
		return buffer[offset];
	}

	@Override
	public void returnEvent(T event) {
		for (; ; ) {
			long writerIndex = this._writerIndex;
			if (writerIndex >= this._readerIndex) {
				Debug.warn("队列已满");
				return;
			}
			int offset = (int) (writerIndex & readerMark);
			if (AA.compareAndSet(_buffer, offset, null, event)) {
				this._writerIndex = writerIndex + 1;
				return;
			}
		}
	}
}
