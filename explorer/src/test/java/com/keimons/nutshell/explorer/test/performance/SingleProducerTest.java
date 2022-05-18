package com.keimons.nutshell.explorer.test.performance;

import com.keimons.nutshell.explorer.support.ReorderedExplorer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 单生产者性能测试
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
@DisplayName("单生产者单消费者性能测试")
public class SingleProducerTest {

	/**
	 * 任务数量
	 */
	private static final int TIMES = 1000_0000;

	/**
	 * 测试任务
	 */
	private static final List<Runnable> tasks = new ArrayList<>(TIMES);

	@BeforeAll
	public static void init() {
		AtomicLong time = new AtomicLong();
		tasks.add(() -> time.set(System.currentTimeMillis()));
		for (int i = 0; i < TIMES - 2; i++) {
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
		tasks.add(() -> System.out.println(Thread.currentThread() + ": " + (System.currentTimeMillis() - time.get())));
	}

	@DisplayName("Executor测试")
	@Test
	public void testExecutor() throws InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(1);
		for (int i = 0; i < TIMES; i++) {
			executor.execute(tasks.get(i));
		}
		Thread.sleep(2000);
	}

	@DisplayName("Explorer测试")
	@Test
	public void testExplorer() throws InterruptedException, ExecutionException {
		ReorderedExplorer explorer = new ReorderedExplorer(1);
		for (int i = 0; i < TIMES; i++) {
			explorer.execute(tasks.get(i), i & 0x1);
		}
		Future<?> future = explorer.close();
		future.get();
	}
}
