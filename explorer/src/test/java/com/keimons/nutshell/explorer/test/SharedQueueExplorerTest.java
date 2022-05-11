package com.keimons.nutshell.explorer.test;

import com.keimons.nutshell.explorer.ExplorerService;
import com.keimons.nutshell.explorer.TrackBarrier;
import com.keimons.nutshell.explorer.internal.BitsTrackBarrier;
import com.keimons.nutshell.explorer.support.BlockPolicy;
import com.keimons.nutshell.explorer.support.SharedQueueExplorer;
import org.junit.jupiter.api.Test;

/**
 * {@link SharedQueueExplorer}测试
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class SharedQueueExplorerTest {

	@Test
	public void test() throws InterruptedException {
		ExplorerService executor = new SharedQueueExplorer("SharedExecutor", 4, new BlockPolicy());
		for (int i = 0; i < 16; i++) {
			TrackBarrier barrier = new BitsTrackBarrier(16);
			barrier.init(0);
			executor.execute(() -> {
				try {
					Thread.sleep(5000);
					System.out.println("execute long");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}, barrier);
		}
		Thread.sleep(2000);
		for (int i = 0; i < 16; i++) {
			int value = i;
			Thread thread = new Thread(() -> {
				System.out.println("thread-" + value + " start");

				TrackBarrier barrier = new BitsTrackBarrier(16);
				barrier.init(0);
				executor.execute(() -> {
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}, barrier);
				System.out.println("thread-" + value + " finish");
			});
			thread.start();
		}
		System.out.println("finish");
		Thread.sleep(30000);
		System.exit(0);
	}
}
