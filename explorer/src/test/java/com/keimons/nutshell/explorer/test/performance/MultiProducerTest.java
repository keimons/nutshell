package com.keimons.nutshell.explorer.test.performance;

import com.keimons.nutshell.explorer.support.ReorderedExplorer;
import com.keimons.nutshell.explorer.test.utils.FillUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能测试
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
public class MultiProducerTest {

	/**
	 * 生产者数量
	 */
	private static final int N_WRITER = 4;

	/**
	 * 消费者数量
	 */
	private static final int N_READER = 2;

	/**
	 * Explorer执行key生成规则
	 * <p>
	 * 生成key：{@code index & MARK}。
	 */
	private static final int MARK = N_READER - 1;

	/**
	 * 任务数量
	 */
	private static final int TIMES = 1000_0000;

	/**
	 * 测试任务
	 */
	private static final List<Runnable> tasks = new ArrayList<>(TIMES);

	/**
	 * 初始化所有任务
	 * <p>
	 * 对于Executor来说，任务执行市场应该是相同的。但是对于Explorer来说，需要输出每个线程执行时长。
	 */
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

	/**
	 * 对比测试Executor执行效率
	 *
	 * @throws InterruptedException 线程中断异常
	 */
	@DisplayName("Executor测试")
	@Test
	public void testExecutor() throws InterruptedException, BrokenBarrierException {
		ExecutorService executor = Executors.newFixedThreadPool(N_READER);
		CyclicBarrier barrier = new CyclicBarrier(N_WRITER + 1);
		for (int i = 0; i < N_WRITER; i++) {
			int start = i;
			Thread thread = new Thread(() -> {
				for (int j = start; j < TIMES; j += N_WRITER) {
					executor.execute(tasks.get(j));
				}
				try {
					barrier.await();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
			thread.start();
		}
		barrier.await();
		executor.shutdown();
		while (!executor.isTerminated()) {
			Thread.sleep(1);
		}
	}

	/**
	 * Explorer执行效率
	 *
	 * @throws InterruptedException 线程中断异常
	 */
	@DisplayName("Explorer测试")
	@Test
	public void testExplorer() throws InterruptedException, BrokenBarrierException, ExecutionException {
		ReorderedExplorer explorer = new ReorderedExplorer(N_READER);
		CyclicBarrier barrier = new CyclicBarrier(N_WRITER + 1);
		for (int i = 0; i < N_WRITER; i++) {
			int start = i;
			Thread thread = new Thread(() -> {
				for (int j = start; j < TIMES; j += N_WRITER) {
					explorer.execute(tasks.get(j), j & MARK);
				}
				try {
					barrier.await();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
			thread.start();
		}
		barrier.await();
		Future<?> close = explorer.close();
		close.get();
	}
}
