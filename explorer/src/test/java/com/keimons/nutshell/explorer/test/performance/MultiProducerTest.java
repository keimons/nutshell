package com.keimons.nutshell.explorer.test.performance;

import com.keimons.nutshell.explorer.support.ReorderedExplorer;
import com.keimons.nutshell.explorer.test.Task;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

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
	private int N_WRITER = 2;

	/**
	 * 消费者数量
	 */
	private int N_READER = 1;

	/**
	 * Explorer执行key生成规则
	 * <p>
	 * 生成key：{@code index & MARK}。
	 */
	private int MARK = N_READER - 1;

	/**
	 * 任务数量
	 */
	private static final int TIMES = 1000_0000;

	/**
	 * 测试任务
	 */
	Runnable TASK = Task::new;

	ThreadLocal<Long> LOCAL = new ThreadLocal<>();

	Runnable TIME = () -> {
		Long start = LOCAL.get();
		if (LOCAL.get() == null) {
			LOCAL.set(System.currentTimeMillis());
		} else {
			long finish = System.currentTimeMillis();
			System.out.println(Thread.currentThread() + ": " + (finish - start));
		}
	};

	public void run(int nWriter, int nReader) throws BrokenBarrierException, InterruptedException, ExecutionException {
		N_WRITER = nWriter;
		N_READER = nReader;
		MARK = N_READER - 1;
		testExecutor();
		testExplorer();
	}

	/**
	 * 对比测试Executor执行效率
	 *
	 * @throws InterruptedException 线程中断异常
	 */
//	@DisplayName("Executor测试")
//	@Test
	public void testExecutor() throws InterruptedException, BrokenBarrierException {
		ExecutorService executor = Executors.newFixedThreadPool(N_READER);
		CyclicBarrier barrier = new CyclicBarrier(N_WRITER + 1);
		for (int i = 0; i < N_WRITER; i++) {
			int start = i;
			Thread thread = new Thread(() -> {
				for (int j = start; j < N_READER; j += N_WRITER) {
					executor.execute(TIME);
				}
				for (int j = start; j < TIMES; j += N_WRITER) {
					executor.execute(TASK);
				}
				for (int j = start; j < N_READER; j += N_WRITER) {
					executor.execute(TIME);
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
//	@DisplayName("Explorer测试")
//	@Test
	public void testExplorer() throws InterruptedException, BrokenBarrierException, ExecutionException {
		ReorderedExplorer explorer = new ReorderedExplorer(N_READER);
		CyclicBarrier barrier = new CyclicBarrier(N_WRITER + 1);
		for (int i = 0; i < N_WRITER; i++) {
			int start = i;
			Thread thread = new Thread(() -> {
				for (int j = start; j < N_READER; j += N_WRITER) {
					explorer.execute(TIME, j);
				}
				for (int j = start; j < TIMES; j += N_WRITER) {
					explorer.execute(TASK, j & MARK);
				}
				for (int j = start; j < N_READER; j += N_WRITER) {
					explorer.execute(TIME, j);
				}
				try {
					barrier.await();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
			thread.start();
		}
		Thread.sleep(2500);
		System.out.println("done.");
		barrier.await();
		Future<?> close = explorer.close();
		close.get();
	}

	@Test
	public void test() throws InterruptedException, BrokenBarrierException, ExecutionException {
		run(4, 2);
		System.gc();
		Thread.sleep(1000);

		System.out.println("writer 1, reader 1");
		run(1, 1);
		System.gc();
		System.gc();
		Thread.sleep(1000);
		System.out.println("writer 2, reader 1");
		run(2, 1);
		System.gc();
		System.gc();
		Thread.sleep(1000);
		System.out.println("writer 4, reader 1");
		run(4, 1);
		System.gc();
		System.gc();
		Thread.sleep(1000);

		System.out.println("writer 1, reader 2");
		run(1, 2);
		System.gc();
		System.gc();
		Thread.sleep(1000);
		System.out.println("writer 2, reader 2");
		run(2, 2);
		System.gc();
		System.gc();
		Thread.sleep(1000);
		System.out.println("writer 4, reader 2");
		run(4, 2);
		System.gc();
		System.gc();
		Thread.sleep(1000);

		System.out.println("writer 2, reader 4");
		run(2, 4);
		System.gc();
		System.gc();
		Thread.sleep(1000);
		System.out.println("writer 4, reader 4");
		run(4, 4);
		System.gc();
		System.gc();
		Thread.sleep(1000);
		System.out.println("writer 8, reader 4");
		run(8, 4);
		System.gc();
		System.gc();
		Thread.sleep(1000);

		System.out.println("writer 4, reader 8");
		run(4, 8);
		System.gc();
		System.gc();
		Thread.sleep(1000);
		System.out.println("writer 8, reader 8");
		run(8, 8);
		System.gc();
		System.gc();
		Thread.sleep(1000);
		System.out.println("writer 16, reader 8");
		run(16, 8);
		System.gc();
		Thread.sleep(1000);
	}
}
