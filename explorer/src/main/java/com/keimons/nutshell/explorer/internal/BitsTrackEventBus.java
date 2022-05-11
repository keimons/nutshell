package com.keimons.nutshell.explorer.internal;

import com.keimons.nutshell.explorer.Debug;
import com.keimons.nutshell.explorer.utils.CASUtils;
import jdk.internal.vm.annotation.Contended;
import jdk.internal.vm.annotation.ForceInline;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 事件总线
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
public class BitsTrackEventBus<T> implements EventBus<T> {

	private static final long OFFSET_WRITER_INDEX = CASUtils.objectFieldOffset(BitsTrackEventBus.class, "writerIndex");

	private static final VarHandle AA = MethodHandles.arrayElementVarHandle(Object[].class);

	final EventFactory<T> factory;

	final int nThreads;

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
	final Node<T>[] buffer;

	volatile long writerIndex;

	final AtomicLong readerIndex = new AtomicLong();

	/**
	 * 缓存系统
	 */
	@Contended
	final T[] _buffer;

	@Contended
	volatile long _readerIndex;

	@Contended
	volatile long _writerIndex;

	/**
	 * 尾指针
	 * <p>
	 * 使用一个模糊判断，当前是否可以写入元素。例如，开启8个线程，发布一个任务，
	 * 任务读取次数应该为8。对于1024队列，如果未读次数大于8192则应该继续读取。
	 */
	AtomicLong adder = new AtomicLong();

	@SuppressWarnings("unchecked")
	public BitsTrackEventBus(EventFactory<T> factory, int capacity, int nThreads) {
		this.nThreads = nThreads;
		this.factory = factory;
		this.capacity = capacity;
		this.writerMark = capacity - 1;
		this.readerMark = (capacity << 1) - 1;
		this._buffer = (T[]) new Object[capacity << 1];
		this.buffer = new Node[capacity];
		fill();
	}

	private void fill() {
		for (int i = 0, length = capacity << 1; i < length; i++) {
			_buffer[i] = factory.newInstance();
		}
		for (int i = 0; i < capacity; i++) {
			this.buffer[i] = new Node<>();
		}
	}

	private long setWriterIndex(long writerIndex) {
		return CASUtils.casSet(this, OFFSET_WRITER_INDEX, writerIndex);
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
	@ForceInline
	public long writerIndex() {
		return writerIndex;
	}

	@Override
	public void publishEvent(T event) {
		while (true) {
			long writerIndex = this.writerIndex;
			int offset = (int) (writerIndex & writerMark);
			Node<T> node = buffer[offset];
			if (node.casState(Node.STATE_FREE, Node.STATE_FULL)) {
				// recheck
				if (writerIndex != this.writerIndex) {
					// rollback state
					node.setState(Node.STATE_FREE);
					continue;
				}
				node.version = writerIndex;
				node.event = event;
				this.writerIndex = writerIndex + 1;
				return;
			} else {
				Thread.yield();
			}
		}
	}

	@Override
	public void finishEvent(long index) {
		int offset = (int) (index & writerMark);
		Node<T> node = buffer[offset];
		node.event = null;
		node.state = Node.STATE_FREE;
	}

	@Override
	public T getEvent(long index) {
		int offset = (int) (index & writerMark);
		Node<T> node = buffer[offset];
		T event = node.event;
		// check version
		long version = node.version;
		if (version != index) {
			return null;
		}
		return event;
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

	private static class Node<T> {

		private static final long STATE = CASUtils.objectFieldOffset(Node.class, "state");

		private static final long EVENT = CASUtils.objectFieldOffset(Node.class, "event");

		private static final long VERSION = CASUtils.objectFieldOffset(Node.class, "version");

		public static final int STATE_FREE = 0;

		public static final int STATE_FULL = 1;

		/**
		 * 节点状态
		 */
		@Contended
		volatile int state = STATE_FREE;

		/**
		 * 装入事件
		 */
		@Contended
		volatile T event;

		@Contended
		volatile long version;

		public boolean casState(int expected, int newValue) {
			return CASUtils.cas(this, STATE, expected, newValue);
		}

		public void setState(int state) {
			CASUtils.casSet(this, STATE, state);
		}

		public void setEvent(Object event) {
			CASUtils.casSet(this, EVENT, event);
		}

		public void setVersion(long version) {
			CASUtils.casSet(this, VERSION, version);
		}
	}
}
