package com.keimons.nutshell.core.bootstrap;

import com.keimons.nutshell.core.ApplicationContext;
import com.keimons.nutshell.core.Hotswappable;
import com.keimons.nutshell.core.assembly.Assembly;
import com.keimons.nutshell.core.assembly.EventType;
import com.keimons.nutshell.core.debug.Debug;
import com.keimons.nutshell.core.internal.utils.ThrowableUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * 链接
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
public class AutolinkBootstrap implements Bootstrap {

	volatile boolean busy = false;

	private final Lock lock = new ReentrantLock();

	@Override
	public void install(ApplicationContext context, Assembly assembly) throws Throwable {

	}

	@Override
	public void hotswap(ApplicationContext context, List<Assembly> inbounds, List<Assembly> outbounds) throws Throwable {
		busy = true;
		List<Thread> threads = new ArrayList<Thread>();
		Consumer<Thread> function = (thread) -> {
			boolean add = false;
			lock.lock();
			try {
				if (busy) {
					Debug.logSafePoint(Debug.SafePoint.PARK, thread.getName());
					threads.add(thread);
					add = true;
				}
			} finally {
				lock.unlock();
			}
			if (add) {
				LockSupport.park();
			}
		};

		// stw
		inbounds.forEach(ThrowableUtils.wrapper((ThrowableUtils.ThrowableConsumer<? super Assembly>) assembly -> assembly.onEvent(EventType.EVENT_STW, true, function)));
		for (; ; ) {
			if (inbounds.stream().allMatch(inbound -> inbound.getHotswappables().stream().mapToInt(Hotswappable::refCnt).sum() == 0)) {
				Debug.logSafePoint(Debug.SafePoint.READY);
				inbounds.forEach(ThrowableUtils.wrapper((ThrowableUtils.ThrowableConsumer<? super Assembly>) assembly -> assembly.onEvent(EventType.EVENT_HOTSWAP)));
				break;
			} else {
				// yield
				Thread.yield();
			}
		}
		inbounds.forEach(ThrowableUtils.wrapper((ThrowableUtils.ThrowableConsumer<? super Assembly>) assembly -> assembly.onEvent(EventType.EVENT_STW, false, null)));
		lock.lock();
		busy = false;
		lock.unlock();
		for (Thread thread : threads) {
			Debug.logSafePoint(Debug.SafePoint.UNPARK, thread.getName());
			LockSupport.unpark(thread);
		}
		// hotswap
		outbounds.addAll(inbounds);
	}
}
