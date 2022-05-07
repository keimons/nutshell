package com.keimons.nutshell.explorer.support;

import com.keimons.nutshell.explorer.Interceptor;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.locks.LockSupport;

/**
 * 休眠拦截器
 * <p>
 * 被拦截的线程进入休眠状态，等待已通行的线程唤醒。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class ParkInterceptor implements Interceptor {

	private static final VarHandle AA = MethodHandles.arrayElementVarHandle(Thread[].class);
	/**
	 * 正在等待被唤醒的线程
	 */
	protected volatile Thread[] threads;
	/**
	 * 拦截量
	 */
	private int forbids;
	/**
	 * 写入位置
	 */
	private volatile int writeIndex;

	@Override
	public void init(int forbids) {
		this.forbids = forbids;
		if (threads == null || forbids > threads.length) {
			this.threads = new Thread[forbids];
		}
	}

	@Override
	public boolean intercept() {
		if (tryIntercept()) {
			LockSupport.park();
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 尝试拦截当前线程
	 * <p>
	 * 对于拦截量以内的线程进行拦截，后续线程放行。
	 *
	 * @return {@code true}拦截成功，{@code false}拦截失败
	 */
	@Override
	public boolean tryIntercept() {
		Thread thread = Thread.currentThread();
		int writeIndex;
		for (; ; ) {
			writeIndex = this.writeIndex;
			if (writeIndex >= forbids) {
				// 已满足拦截量，拦截失败
				return false;
			}
			if (AA.compareAndExchange(threads, writeIndex, null, thread) == null) {
				this.writeIndex = writeIndex + 1;
				return true;
			}
		}
	}

	@Override
	public boolean isIntercepted() {
		return true;
	}

	@Override
	public void release() {
		for (int i = 0; i < forbids; i++) {
			LockSupport.unpark(threads[i]);
			threads[i] = null;
		}
	}
}
