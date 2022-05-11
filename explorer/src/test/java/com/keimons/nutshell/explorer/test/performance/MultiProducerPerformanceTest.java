package com.keimons.nutshell.explorer.test.performance;

import com.keimons.nutshell.explorer.support.ReorderedExplorer;
import com.keimons.nutshell.explorer.test.utils.FillUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能测试
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
public class MultiProducerPerformanceTest {

	private static final int N_WRITER = 16;

	private static final int N_READER = 8;

	private static final int MARK = N_READER - 1;

	private static final int TIMES = 1000_0000;

	private static List<Runnable> tasks = new ArrayList<>(TIMES);

	@BeforeAll
	public static void beforeTest() {
		AtomicLong[] times = (AtomicLong[]) Array.newInstance(AtomicLong.class, N_READER);
		FillUtils.fill(times, AtomicLong::new);
		for (int i = 0; i < TIMES; i++) {
			int index = i;
			if (i < N_READER) {
				tasks.add(() -> times[index].set(System.currentTimeMillis()));
			} else if (i >= TIMES - N_READER) {
				tasks.add(() -> System.out.println(Thread.currentThread() + "Thread: " + (System.currentTimeMillis() - times[index - (TIMES - N_READER)].get())));
			} else {
				tasks.add(() -> {
				});
			}
		}
	}

	@DisplayName("Executor测试")
	@Test
	public void testExecutor() throws InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(N_READER);
		for (int i = 0; i < N_WRITER; i++) {
			int start = i;
			Thread thread = new Thread(() -> {
				for (int j = start; j < tasks.size(); j += N_WRITER) {
					executor.execute(tasks.get(j));
				}
			});
			thread.start();
		}
		Thread.sleep(5000);
	}

	@DisplayName("Explorer测试")
	@Test
	public void testExplorer() throws InterruptedException {
		ReorderedExplorer explorer = new ReorderedExplorer(N_READER);
		for (int i = 0; i < N_WRITER; i++) {
			int start = i;
			Thread thread0 = new Thread(() -> {
				for (int j = start; j < tasks.size(); j += N_WRITER) {
					explorer.execute(tasks.get(j), j & MARK);
				}
			});
			thread0.start();
		}
		Thread.sleep(5000);
	}
}
