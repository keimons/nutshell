package com.keimons.nutshell.disruptor.support;

import com.keimons.nutshell.disruptor.Interceptor;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 重排序拦截器
 * <p>
 * 拦截器的一种实现，支持消息的重排序执行。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class ReorderedInterceptor implements Interceptor {

	private final AtomicInteger forbids = new AtomicInteger();

	private volatile boolean intercepted;

	@Override
	public void init(int forbids) {
		this.intercepted = true;
		this.forbids.set(forbids);
	}

	@Override
	public boolean intercept() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean tryIntercept() {
		return forbids.getAndDecrement() > 0;
	}

	@Override
	public boolean isIntercepted() {
		return intercepted;
	}

	@Override
	public void release() {
		intercepted = false;
	}
}
