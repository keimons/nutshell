package com.keimons.nutshell.explorer.internal;

import com.keimons.nutshell.explorer.Debug;
import jdk.internal.vm.annotation.Contended;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * 缓存管理
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
public class CacheManager<T> {

	private static final VarHandle AA = MethodHandles.arrayElementVarHandle(Object[].class);

	final EventFactory<T> factory;

	final int _mark;

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
	public CacheManager(int capacity, EventFactory<T> factory) {
		this.factory = factory;
		this._mark = capacity - 1;
		this._buffer = (T[]) new Object[capacity];
	}

	public T borrowEvent() {
		for (; ; ) {
			long readerIndex = this._readerIndex;
			int offset = (int) (readerIndex & _mark);
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

	public void returnEvent(T event) {
		for (; ; ) {
			long writerIndex = this._writerIndex;
			if (writerIndex >= this._readerIndex) {
				Debug.warn("队列已满");
				return;
			}
			int offset = (int) (writerIndex & _mark);
			if (AA.compareAndSet(_buffer, offset, null, event)) {
				this._writerIndex = writerIndex + 1;
				return;
			}
		}
	}
}
