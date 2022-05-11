package com.keimons.nutshell.explorer.test.performance;

import com.keimons.nutshell.explorer.support.AbortPolicy;
import com.keimons.nutshell.explorer.support.ReorderedExplorer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 单生产者性能测试
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
@DisplayName("单生产者单消费者性能测试")
public class SingleProducerPerformanceTest {

	static List<Runnable> tasks = new ArrayList<>(1000000);

	@BeforeAll
	public static void init() {
		AtomicLong time = new AtomicLong();
		tasks.add(() -> time.set(System.currentTimeMillis()));
		for (int i = 0; i < 999998; i++) {
			int v = i;
			tasks.add(new Runnable() {
				@Override
				public void run() {

				}

				public String toString() {
					return String.valueOf(v);
				}
			});
		}
		tasks.add(() -> {
			System.out.println(Thread.currentThread() + ": " + (System.currentTimeMillis() - time.get()));
		});
	}

	@DisplayName("Executor测试")
	@Test
	public void testExecutor() throws InterruptedException {
		ExecutorService executor0 = Executors.newFixedThreadPool(1);
		Thread.sleep(100);
		for (int i = 0; i < tasks.size(); i++) {
			executor0.execute(tasks.get(i));
		}
		Thread.sleep(1000);
	}

	@DisplayName("Explorer测试")
	@Test
	public void testExplorer() throws InterruptedException {
		ReorderedExplorer executor1 = new ReorderedExplorer("ReorderedTrackExecutor", 1, 1024, new AbortPolicy());
		Thread.sleep(100);
		for (int i = 0; i < tasks.size(); i++) {
			executor1.execute(tasks.get(i), i & 0x1);
		}
		Thread.sleep(1000);
	}
}
