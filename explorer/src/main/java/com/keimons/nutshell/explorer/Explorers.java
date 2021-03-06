package com.keimons.nutshell.explorer;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Explorers
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
public class Explorers {

	public static ThreadFactory defaultThreadFactory() {
		return new DefaultThreadFactory();
	}

	private static class DefaultThreadFactory implements ThreadFactory {

		private static final AtomicInteger poolNumber = new AtomicInteger(0);

		private final ThreadGroup group;

		private final AtomicInteger threadNumber = new AtomicInteger(0);

		private final String namePrefix;

		DefaultThreadFactory() {
			group = Thread.currentThread().getThreadGroup();
			namePrefix = "explorer-" +
					poolNumber.getAndIncrement() +
					"-thread-";
		}

		public Thread newThread(Runnable r) {
			Thread t = new Thread(group, r,
					namePrefix + threadNumber.getAndIncrement(),
					0);
			if (t.isDaemon()) {
				t.setDaemon(false);
			}
			if (t.getPriority() != Thread.NORM_PRIORITY) {
				t.setPriority(Thread.NORM_PRIORITY);
			}
			return t;
		}
	}
}
