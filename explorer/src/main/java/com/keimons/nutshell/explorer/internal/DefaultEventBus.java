package com.keimons.nutshell.explorer.internal;

import com.keimons.nutshell.explorer.utils.XUtils;
import jdk.internal.vm.annotation.Contended;

import java.lang.invoke.VarHandle;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 事件总线
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
public class DefaultEventBus<T> implements EventBus<T> {

//	private static final VarHandle L = XUtils.findVarHandle(DefaultEventBus.class, "writerIndex", long.class);

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
	 * 允许写入的最后一个位置
	 */
	volatile long limitIndex = Long.MAX_VALUE;

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

	AtomicBoolean mainLock = new AtomicBoolean();

	@Override
	public long writerIndex() {
		return writerIndex;
	}

	@Override
	public boolean publishEvent(T event) {
		while (true) {
			if (mainLock.compareAndSet(false, true)) {
				try {
					long writerIndex = this.writerIndex;
					if (writerIndex < limitIndex) {
						int offset = (int) (writerIndex & mark);
						Node<T> node = buffer[offset];
						if (node.event != null) {
							continue;
						}
						node.version = writerIndex;
						node.event = event;
						this.writerIndex = writerIndex + 1;
						return true;
					} else {
						return false;
					}
				} finally {
					mainLock.set(false);
				}
			} else {
				Thread.yield();
			}

//			long writerIndex = this.writerIndex;
//			int offset = (int) (writerIndex & mark);
//			Node<T> node = buffer[offset];
//			if (node.casState(Node.STATE_FREE, Node.STATE_FULL)) {
//				// recheck
//				if (writerIndex != this.writerIndex) {
//					// rollback state
//					node.state = Node.STATE_FREE;
//					continue;
//				}
//				if (writerIndex < limitIndex) {
//					node.version = writerIndex;
//					node.event = event;
//					this.writerIndex = writerIndex + 1;
//					return true;
//				} else {
//					return false;
//				}
//			} else {
//				// TODO 调用拒绝策略
//				Thread.yield();
//			}
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

	/**
	 * 线程安全的
	 * <p>
	 * 同时有且仅只有一个线程在完成事件。
	 *
	 * @param index 事件索引
	 */
	@Override
	public void finishEvent(long index) {
		int offset = (int) (index & mark);
		Node<T> node = buffer[offset];
		node.event = null;
		node.state = Node.STATE_FREE;
	}

	@Override
	public boolean testWriterIndex(long writerIndex) {
		return writerIndex >= limitIndex;
	}

	@Override
	public void shutdown() {
		if (mainLock.compareAndSet(false, true)) {
			limitIndex = writerIndex;
			mainLock.set(false);
		}
	}

	/**
	 * 环形缓冲区中的节点
	 * <p>
	 * 节点在缓冲区创建时初始化，直到事件总线被回收时销毁。
	 * <p>
	 * 节点中的字段，有严格的设置顺序，例如在{@link #publishEvent(Object)}中的设置：
	 * {@link #state} -> {@link #version} -> {@link #event}这个顺序是不能更改的，
	 * 和严格的修改顺序{@link #event}
	 *
	 * @param <T>
	 */
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
