package com.keimons.nutshell.explorer.test.explorer;

import com.keimons.nutshell.explorer.ConsumerTask;
import com.keimons.nutshell.explorer.support.Threadripper;
import org.junit.jupiter.api.Test;

/**
 * {@link Threadripper}关闭测试
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
public class ThreadripperShutdownTest {

	@Test
	public void test() {
		Threadripper explorer = new Threadripper(4);

		explorer.shutdown(new ConsumerTask<>((tasks) -> {
			for (Runnable task : tasks) {
				task.run();
			}
		}));
	}
}
