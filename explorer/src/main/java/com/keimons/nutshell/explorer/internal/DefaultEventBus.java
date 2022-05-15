package com.keimons.nutshell.explorer.internal;

import com.keimons.nutshell.explorer.utils.XUtils;
import jdk.internal.vm.annotation.Contended;
import jdk.internal.vm.annotation.ForceInline;

import java.lang.invoke.VarHandle;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 事件总线
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
public class DefaultEventBus<T> implements EventBus<T> {

	final int capacity;

	final int mark;

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

	/**
	 * 记录已读取事件数量
	 * <p>
	 * 这是一个计数值，用于计数已经处理了多少事件。（未完成设计）
	 */
	@Deprecated
	private final AtomicLong record = new AtomicLong();

	@SuppressWarnings("unchecked")
	public DefaultEventBus(int capacity) {
		this.capacity = capacity;
		this.mark = capacity - 1;
		this.buffer = new Node[capacity];
		this.fill();
	}

	private void fill() {
		for (int i = 0; i < capacity; i++) {
			this.buffer[i] = new Node<>();
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
			int offset = (int) (writerIndex & mark);
			Node<T> node = buffer[offset];
			if (node.casState(Node.STATE_FREE, Node.STATE_FULL)) {
				// recheck
				if (writerIndex != this.writerIndex) {
					// rollback state
					node.state = Node.STATE_FREE;
					continue;
				}
				node.version = writerIndex;
				node.event = event;
				this.writerIndex = writerIndex + 1;
				return;
			} else {
				// TODO 调用拒绝策略
				Thread.yield();
			}
		}
	}

	@Override
	public T getEvent(long index) {
		int offset = (int) (index & mark);
		Node<T> node = buffer[offset];
		T event = node.event;
		long version = node.version;
		// check version
		if (version != index) {
			return null;
		}
		return event;
	}

	@Override
	public void finishEvent(long index) {
		int offset = (int) (index & mark);
		Node<T> node = buffer[offset];
		node.event = null;
		node.state = Node.STATE_FREE;
	}

	private static class Node<T> {

		private static final VarHandle II = XUtils.findVarHandle(Node.class, "state", int.class);

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
			return II.compareAndSet(this, expected, newValue);
		}
	}
}
