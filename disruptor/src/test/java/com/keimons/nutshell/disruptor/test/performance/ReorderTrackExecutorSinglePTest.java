package com.keimons.nutshell.disruptor.test.performance;

import com.keimons.nutshell.disruptor.support.AbortPolicy;
import com.keimons.nutshell.disruptor.support.ReorderedTrackExecutor;
import org.junit.jupiter.api.Test;

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
public class ReorderTrackExecutorSinglePTest {

	@Test
	public void test() throws InterruptedException {
		ExecutorService executor0 = Executors.newFixedThreadPool(4);

		ReorderedTrackExecutor executor1 = new ReorderedTrackExecutor("ReorderedTrackExecutor", 4, 1024, new AbortPolicy());

		List<Runnable> tasks = new ArrayList<>(1000000);
		AtomicLong time = new AtomicLong();
		tasks.add(() -> time.set(System.currentTimeMillis()));
		for (int i = 0; i < 999998; i++) {
			tasks.add(() -> {});
		}
		tasks.add(() -> {
			System.out.println(System.currentTimeMillis() - time.get());
		});
		Thread.sleep(1000);
		System.gc();
		for (int i = 0; i < tasks.size(); i++) {
			executor0.execute(tasks.get(i));
		}
//		Thread.sleep(1000);
//		for (int i = 0; i < tasks.size(); i++) {
//			executor0.execute(tasks.get(i));
//		}
//		Thread.sleep(1000);
		System.gc();
		Thread.sleep(1000);
		for (int i = 0; i < tasks.size(); i++) {
			executor1.execute(tasks.get(i), i & 1);
		}
	}
}
