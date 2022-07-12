package com.keimons.nutshell.explorer.support;

import com.keimons.nutshell.core.Interceptor;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 超高性能拦截器
 * <p>
 * 依赖{@link Thread#yield()}实现，提供仅次于死循环的Top1级别性能（注：暂不考虑提供Top0的死循环）。
 * <p>
 * 使用推荐：
 * <ul>
 *     <li>1. CPU密集型任务</li>
 *     <li>2. 拦截量偏小</li>
 * </ul>
 * 在拥有足够性能的同时，为防止过度浪费CPU而设计，但它依然是浪费性能的且不可靠的。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
public class YieldInterceptor implements Interceptor {

	/**
	 * 拦截量
	 * <p>
	 * 每拦截一个线程，拦截量-1。当剩余拦截量{@code forbids <= 0}时，不再进行拦截。
	 */
	private final AtomicInteger forbids = new AtomicInteger();

	/**
	 * 是否全放行
	 * <p>
	 * 当某个线程执行{@link #release()}后，放行所有正在拦截的线程。
	 */
	private volatile boolean release;

	public YieldInterceptor(int forbids) {
		this.release = false;
		this.forbids.set(forbids);
	}

	@Override
	public boolean tryIntercept() {
		return forbids.decrementAndGet() <= 0;
	}

	@Override
	public boolean isIntercepted() {
		return release;
	}

	@Override
	public void release() {
		release = true;
	}
}
