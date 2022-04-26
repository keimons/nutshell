package com.keimons.nutshell.disruptor.test;

import com.keimons.nutshell.disruptor.TrackBarrier;
import com.keimons.nutshell.disruptor.TrackExecutor;
import com.keimons.nutshell.disruptor.support.BitTrackBarrier;
import com.keimons.nutshell.disruptor.support.BlockPolicy;
import com.keimons.nutshell.disruptor.support.SharedQueueExecutor;
import org.junit.jupiter.api.Test;

/**
 * {@link SharedQueueExecutor}测试
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class SharedQueueExecutorTest {

	@Test
	public void test() throws InterruptedException {
		TrackExecutor executor = new SharedQueueExecutor("SharedExecutor", 4, new BlockPolicy());
		for (int i = 0; i < 16; i++) {
			TrackBarrier barrier = new BitTrackBarrier(16);
			barrier.init(0);
			executor.execute(barrier, () -> {
				try {
					Thread.sleep(5000);
					System.out.println("execute long");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			});
		}
		Thread.sleep(2000);
		for (int i = 0; i < 16; i++) {
			int value = i;
			Thread thread = new Thread(() -> {
				System.out.println("thread-" + value + " start");

				TrackBarrier barrier = new BitTrackBarrier(16);
				barrier.init(0);
				executor.execute(barrier, () -> {
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				});
				System.out.println("thread-" + value + " finish");
			});
			thread.start();
		}
		System.out.println("finish");
		Thread.sleep(30000);
		System.exit(0);
	}
}
