package com.keimons.nutshell.dispenser.test;

import com.keimons.nutshell.dispenser.HashExecutor;
import com.keimons.nutshell.dispenser.support.BlockPolicy;
import com.keimons.nutshell.dispenser.support.SharedQueueExecutor;
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
		HashExecutor executor = new SharedQueueExecutor("SharedExecutor", 4, new BlockPolicy());
		for (int i = 0; i < 16; i++) {
			executor.execute(0, () -> {
				try {
					Thread.sleep(10000);
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
				executor.execute(0, () -> {
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
