package com.keimons.nutshell.explorer.support;

import com.keimons.nutshell.explorer.Interceptor;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 重排序拦截器
 * <p>
 * 拦截器的一种实现，支持消息的重排序执行。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
public class ReorderedInterceptor implements Interceptor {

	private final AtomicInteger forbids = new AtomicInteger();

	private volatile boolean intercepted;

	public ReorderedInterceptor(int forbids) {
		this.intercepted = true;
		this.forbids.set(forbids);
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
