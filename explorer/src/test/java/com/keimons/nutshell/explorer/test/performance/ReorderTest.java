package com.keimons.nutshell.explorer.test.performance;

import com.keimons.nutshell.explorer.Debug;
import com.keimons.nutshell.explorer.support.ReorderedExplorer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@link ReorderedExplorer}重排序性能测试
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
public class ReorderTest {

	private static final int THREAD = 4;

	private static final int BARRIER = 0;

	/**
	 * 测试次数
	 */
	private static final int COUNT = 1000_0000;

	@BeforeAll
	public static void beforeTest() {
		Debug.DEBUG = true;
	}

	@DisplayName("重排序任务性能测试")
	@Test
	public void test() throws InterruptedException {
		ReorderedExplorer explorer = new ReorderedExplorer(ReorderedExplorer.DEFAULT_NAME,
				THREAD,
				THREAD * ReorderedExplorer.DEFAULT_THREAD_CAPACITY,
				ReorderedExplorer.DefaultRejectedHandler,
				new IndexThreadFactory()
		);
		AtomicInteger busy = new AtomicInteger();
		AtomicBoolean ready = new AtomicBoolean();
		if (BARRIER > 0) {
			explorer.execute(() -> {
				busy.set(Integer.parseInt(Thread.currentThread().getName()));
				ready.set(true);
				try {
					Thread.sleep(1000000);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}, 0, 1);
		} else {
			ready.set(true);
			busy.set(0);
		}
		for (; ; ) {
			if (ready.get()) {
				final int mark = busy.get() == 0 ? 1 : 0;
				for (int i = 0; i < BARRIER - 1; i++) {
					explorer.execute(() -> {
					}, mark, mark + THREAD, mark + 2 * THREAD);
				}
				// 已初始化100个屏障，开始越过屏障性能测试
				AtomicLong time = new AtomicLong();
				explorer.execute(() -> time.set(System.currentTimeMillis()), mark + THREAD);
				for (int i = 0; i < COUNT - 2; i++) {
					explorer.execute(() -> {
					}, mark + THREAD << 2);
				}
				explorer.execute(() -> System.out.println(Thread.currentThread() + "Thread: " + (System.currentTimeMillis() - time.get())), mark + THREAD);
				break;
			}
		}
		Thread.sleep(10000);
	}

	private static class IndexThreadFactory implements ThreadFactory {
		private static final AtomicInteger poolNumber = new AtomicInteger(1);
		private final ThreadGroup group;
		private final AtomicInteger Index = new AtomicInteger(0);

		public IndexThreadFactory() {
			SecurityManager s = System.getSecurityManager();
			group = (s != null) ? s.getThreadGroup() :
					Thread.currentThread().getThreadGroup();
		}

		@Override
		public Thread newThread(@NotNull Runnable r) {
			Thread t = new Thread(group, r, String.valueOf(Index.getAndIncrement()), 0);
			if (t.isDaemon())
				t.setDaemon(false);
			if (t.getPriority() != Thread.NORM_PRIORITY)
				t.setPriority(Thread.NORM_PRIORITY);
			return t;
		}
	}
}
