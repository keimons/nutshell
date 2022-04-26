package com.keimons.nutshell.disruptor.support.event;

import com.keimons.nutshell.disruptor.Event;
import com.keimons.nutshell.disruptor.Interceptor;
import com.keimons.nutshell.disruptor.TrackBarrier;
import com.keimons.nutshell.disruptor.TrackBuffer;
import com.keimons.nutshell.disruptor.support.ReorderedInterceptor;
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
public class EventBus<E extends Event> implements TrackBuffer {

	private static final int ARRAY_PADDING = 16;

	private static final VarHandle AA = MethodHandles.arrayElementVarHandle(Object[].class);

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
	@Contended
	final Node[] buffer;
	volatile long writerIndex;

	public EventBus(int capacity) {
		buffer = new Node[capacity];
		mark = capacity - 1;
	}

	@Override
	public long getWriterIndex() {
		return writerIndex;
	}

	@Override
	public void publish(TrackBarrier barrier, Runnable event) {
		Node node = new Node();
		node.barrier = barrier;
		node.task = event;
		node.interceptor = new ReorderedInterceptor();
		node.interceptor.init(barrier.intercept());
		while (true) {
			long index = this.writerIndex;
			int offset = (int) (index & mark);
			if (AA.compareAndSet(buffer, offset, null, node)) {
				this.writerIndex = index + 1;
				return;
			} else {
				Thread.yield();
			}
		}
	}

	@Override
	public Node get(long index) {
		int offset = (int) (index & mark);
		return buffer[offset];
	}

	@Override
	public void remove(long index) {
		int offset = (int) (index & mark);
		buffer[offset] = null;
	}

	public Event get(int index) {
		return buffer[index & mark];
	}

	public static class Node extends Event {

		public Runnable task;

		public Interceptor interceptor;

		public TrackBarrier barrier;
	}
}
