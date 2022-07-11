package com.keimons.nutshell.explorer.test;

import com.keimons.nutshell.explorer.Debug;
import com.keimons.nutshell.explorer.support.Threadripper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * {@link Threadripper}重排序轨道执行器测试
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
public class ThreadripperTest {

	@BeforeAll
	public static void beforeTest() {
		Debug.DEBUG = true;
	}

	@Test
	public void test() throws InterruptedException, ExecutionException {
		Threadripper explorer = new Threadripper(4);
		explorer.execute(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				System.out.println(this + " execute by: " + Thread.currentThread());
			}

			@Override
			public String toString() {
				return "共享任务(KEY-0, KEY-1)";
			}
		}, 0, 1);

		explorer.execute(new Runnable() {
			@Override
			public void run() {
				System.out.println(this + " execute by: " + Thread.currentThread());
			}

			@Override
			public String toString() {
				return "串行任务(KEY-0)";
			}
		}, 0);

		explorer.execute(new Runnable() {
			@Override
			public void run() {
				System.out.println(this + " execute by: " + Thread.currentThread());
			}

			@Override
			public String toString() {
				return "串行任务(KEY-1)";
			}
		}, 1);

		explorer.execute(new Runnable() {
			@Override
			public void run() {
				System.out.println(this + " execute by: " + Thread.currentThread());
			}

			@Override
			public String toString() {
				return "重排任务(KEY-4)";
			}
		}, 4);

		explorer.execute(new Runnable() {
			@Override
			public void run() {
				System.out.println(this + " execute by: " + Thread.currentThread());
			}

			@Override
			public String toString() {
				return "重排任务(KEY-5)";
			}
		}, 5);
		FutureTask<?> onClose = new FutureTask<>(() -> System.out.println("线程池已关闭"), null);
		explorer.close(onClose);
		onClose.get();
	}

	@DisplayName("顺序测试")
	@Test
	public void testOrdered() throws InterruptedException, ExecutionException {
		ThreadLocal<Integer> LOCAL = new ThreadLocal<>();
		Threadripper explorer = new Threadripper(4);
		explorer.execute(() -> LOCAL.set(-4), 0);
		explorer.execute(() -> LOCAL.set(-3), 1);
		explorer.execute(() -> LOCAL.set(-2), 2);
		explorer.execute(() -> LOCAL.set(-1), 3);
		for (int i = 0; i < 10000000; i++) {
			final int value = i;
			explorer.execute(() -> {
				if (value - LOCAL.get() != 4) {
					System.err.println("ordered failed.");
				}
				LOCAL.set(value);
			}, i & 3);
		}
		explorer.execute(() -> System.out.println(Thread.currentThread() + ": done."), 0);
		explorer.execute(() -> System.out.println(Thread.currentThread() + ": done."), 1);
		explorer.execute(() -> System.out.println(Thread.currentThread() + ": done."), 2);
		explorer.execute(() -> System.out.println(Thread.currentThread() + ": done."), 3);
		FutureTask<?> onClose = new FutureTask<>(() -> System.out.println("线程池已关闭"), null);
		explorer.close(onClose);
		onClose.get();
	}
}
