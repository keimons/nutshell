package com.keimons.nutshell.explorer.test;

import com.keimons.nutshell.explorer.support.AbortPolicy;
import com.keimons.nutshell.explorer.support.ReorderedExplorer;
import org.junit.jupiter.api.Test;

/**
 * {@link ReorderedExplorer}重排序轨道执行器测试
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class ReorderedExplorerTest {

	@Test
	public void test() throws InterruptedException {
		ReorderedExplorer executor = new ReorderedExplorer("ReorderedTrack", 1, 1024, new AbortPolicy());
		executor.execute(new Runnable() {
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

		executor.execute(new Runnable() {
			@Override
			public void run() {
				System.out.println(this + " execute by: " + Thread.currentThread());
			}

			@Override
			public String toString() {
				return "串行任务(KEY-0)";
			}
		}, 0);

		executor.execute(new Runnable() {
			@Override
			public void run() {
				System.out.println(this + " execute by: " + Thread.currentThread());
			}

			@Override
			public String toString() {
				return "串行任务(KEY-1)";
			}
		}, 1);

		executor.execute(new Runnable() {
			@Override
			public void run() {
				System.out.println(this + " execute by: " + Thread.currentThread());
			}

			@Override
			public String toString() {
				return "重排任务(KEY-4)";
			}
		}, 4);

		executor.execute(new Runnable() {
			@Override
			public void run() {
				System.out.println(this + " execute by: " + Thread.currentThread());
			}

			@Override
			public String toString() {
				return "重排任务(KEY-5)";
			}
		}, 5);

		Thread.sleep(10000);
	}
}
