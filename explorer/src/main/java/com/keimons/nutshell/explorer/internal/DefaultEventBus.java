package com.keimons.nutshell.explorer.internal;

import com.keimons.nutshell.explorer.utils.XUtils;
import jdk.internal.vm.annotation.Contended;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.VarHandle;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 默认事件总线
 * <p>
 * 事件总线是一个环形buffer的实现。与普通环形buffer不同的是，事件总线在抽象概念上存在，事实上，事件总线并不存在。
 * 事件总线发布的事件有这几个特点：
 * <ul>
 *     <li>并不是严格按照事件的发布顺序消费；</li>
 *     <li>线程总是在遍历整个事件总线；</li>
 *     <li>事件可能由多个线程持有，最终只会由一个线程消费并释放；</li>
 *     <li>事件总线允许被关闭，但是关闭并不会清空所有事件；</li>
 *     <li>最坏情况下，如果一个事件没能被消费，那么它将卡主整个事件总线（解决方案：链表）。</li>
 * </ul>
 * 事件在事件总线中连续发布，任何时刻，事件总线中的事件都不一定是连续存在的，某一时刻：
 * <pre>
 *     +-----+    +-----+    +-----+    +-----+    +-----+    +-----+
 *     |     | <- |  Y  | <- |  N  | <- |  N  | <- |  Y  | <- |  N  |
 *     +-----+    +-----+    +-----+    +-----+    +-----+    +-----+
 *     |
 *     writerIndex
 * </pre>
 * 这使得，如果一个节点没有被释放，那么当环形buffer写满一圈后，将无法继续写入，使得这个环形buffer卡死。
 * 尽管我们可以在此处添加链表实现，防止环形buffer卡死，事实上，我们更希望Explorer处理CPU密集型任务，
 * 为此，我们添加了守护线程，用于监听耗时较长的任务。
 * <p>
 * <p>
 * 注意：默认的事件总线的实现，选择了仅次于无限循环的{@link Thread#yield()}让出CPU重新竞争的方案。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
public class DefaultEventBus<T> implements EventBus<T> {

	/**
	 * 默认最大队列长度
	 */
	private static final long DEFAULT_LIMIT = Long.MAX_VALUE;

	/**
	 * 容量
	 * <p>
	 * 缓冲区没有设计链表之前，这个容量就是缓冲区的最大容量。尽管容量达到了最大，特殊的消费形式，
	 * 决定了缓冲区内部依然可能有剩余空间。
	 */
	private final int capacity;

	/**
	 * 索引比特位标记
	 * <p>
	 * 环形缓冲区特有的一个标记，通常是{@code capacity - 1}，通过{@code writerIndex & mark}获取真实的写入位置。
	 */
	private final int mark;

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
	private final Node<T>[] buffer;

	/**
	 * 主锁（未使用）
	 * <p>
	 * 主锁的存在是为了在关闭缓冲区时，能够锁定缓冲区。
	 */
	@Deprecated
	private final AtomicBoolean mainLock = new AtomicBoolean();

	/**
	 * 当前的写入位置
	 * <p>
	 * 写入位置需要竞争，在多线程环境下，竞争到写入位置时，才能发布事件在事件总线。
	 * 在没有链表的情况下，始终判断{@code writerIndex + 1}的位置是否可以写入事件，
	 * 如果该位置可以写入，则写入事件，如果不能写入，则调用等待策略。
	 */
	volatile long writerIndex;

	/**
	 * 允许写入的最后一个位置
	 * <p>
	 * 通过判断{@code writerIndex < limitIndex}时，可以向缓冲区中写入数据。
	 * 在运行时，这个值固定为{@link #DEFAULT_LIMIT}，当关闭缓冲区时，此值更新为{@link #writerIndex}。
	 * <p>
	 * 缓冲区中允许写入最多2<sup>63</sup>-1(9223372036854775807)个元素，完全足够。
	 */
	volatile long limitIndex = DEFAULT_LIMIT;

	@SuppressWarnings("unchecked")
	public DefaultEventBus(int capacity) {
		this.capacity = capacity;
		this.mark = capacity - 1;
		this.buffer = new Node[capacity];
		this.fill();
	}

	/**
	 * 填充缓冲区
	 */
	private void fill() {
		for (int i = 0; i < capacity; i++) {
			this.buffer[i] = new Node<>();
		}
	}

	@Override
	public long writerIndex() {
		return writerIndex;
	}

	@Override
	public boolean publishEvent(T event) {
		while (true) {
			long sequence = writerIndex;
			int offset = (int) (sequence & mark);
			Node<T> node = buffer[offset];
			if (node.casState(Node.STATE_FREE, Node.STATE_FULL)) {
				// recheck
				if (sequence != writerIndex) {
					// rollback state
					node.state = Node.STATE_FREE;
					continue;
				}
				if (sequence >= limitIndex) {
					return false;
				}
				node.sequence = sequence;
				node.event = event;
				writerIndex = sequence + 1;
				return true;
			} else {
				// TODO 调用拒绝策略
				Thread.yield();
			}
		}
	}

	@Override
	public @Nullable T getEvent(long sequence) {
		int offset = (int) (sequence & mark);
		Node<T> node = buffer[offset];
		T event = node.event;
		long version = node.sequence;
		// check version
		if (version != sequence) {
			return null;
		}
		return event;
	}

	@Override
	public void removeEvent(long sequence) {
		int offset = (int) (sequence & mark);
		Node<T> node = buffer[offset];
		node.event = null;
		node.state = Node.STATE_FREE;
	}

	@Override
	public boolean eof(long readerIndex) {
		return readerIndex >= limitIndex;
	}

	@Override
	public void shutdown() {
		if (limitIndex != DEFAULT_LIMIT) {
			return;
		}
		while (true) {
			long sequence = writerIndex;
			int offset = (int) (sequence & mark);
			Node<T> node = buffer[offset];
			if (node.casState(Node.STATE_FREE, Node.STATE_FULL)) {
				// recheck
				if (sequence != writerIndex) {
					// rollback state
					node.state = Node.STATE_FREE;
					continue;
				}
				if (limitIndex != DEFAULT_LIMIT) {
					return;
				}
				limitIndex = writerIndex;
				return;
			}
		}
	}

	/**
	 * 环形缓冲区中的节点
	 * <p>
	 * 节点在缓冲区创建时初始化，伴随着事件总线的销毁而销毁。
	 * <p>
	 * 节点中的字段，有严格的设置顺序，例如在{@link #publishEvent(Object)}中的设置：
	 * <ul>
	 *     <li>锁定顺序：{@link #state} -> {@link #sequence} -> {@link #event}</li>
	 *     <li>释放顺序：{@link #event} -> {@link #state}</li>
	 *     <li>访问顺序：{@link #event} -> {@link #sequence}</li>
	 * </ul>
	 * 这个顺序是不能更改的，
	 * 和严格的修改顺序{@link #event} -> {@link #state}，以及严格的访问顺序
	 *
	 * @param <T>
	 */
	private static class Node<T> {

		private static final VarHandle II = XUtils.findVarHandle(Node.class, "state", int.class);

		/**
		 * 节点状态-空闲中
		 * <p>
		 * 如果节点处于空闲中，则各个线程可以通过cas的方式竞争节点。
		 */
		public static final int STATE_FREE = 0;

		/**
		 * 节点状态-占用中
		 * <p>
		 * 如果节点已经被占用，那么节点只能等待某个线程消耗这个节点。
		 */
		public static final int STATE_FULL = 1;

		/**
		 * 节点状态
		 */
		@Contended
		volatile int state = STATE_FREE;

		/**
		 * 节点存放的事件
		 */
		@Contended
		volatile T event;

		/**
		 * 事件唯一序列
		 * <p>
		 * 事件发布到事件总线后，会为每一个事件生成一个唯一序列（通常是{@link #writerIndex}）。
		 * 不论是查找事件，还是移除事件，都是依靠事件序列来完成的。
		 */
		@Contended
		volatile long sequence;

		/**
		 * 原子的方式更新状态值
		 *
		 * @param expected 预期状态
		 * @param newValue 新的状态
		 * @return {@code true}更新成功，{@code false}更新失败
		 */
		public boolean casState(int expected, int newValue) {
			return II.compareAndSet(this, expected, newValue);
		}
	}
}
