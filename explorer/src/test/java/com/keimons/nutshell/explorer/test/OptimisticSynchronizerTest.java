package com.keimons.nutshell.explorer.test;

import com.keimons.nutshell.core.OptimisticSynchronizer;
import com.keimons.nutshell.explorer.support.Threadripper;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * {@link OptimisticSynchronizer}测试
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 **/
public class OptimisticSynchronizerTest {

	private static final Runnable TASK = Task::new;

	@Test
	public void test() throws ExecutionException, InterruptedException {
		Threadripper explorer = new Threadripper(2);
		for (int i = 0; i < 2_0000_0000; i++) {
			if ((i & 0B11) == 0B11) {
				Future<?> future = explorer.submit(TASK, 0);
				future.get();
			} else {
				explorer.execute(TASK, 0);
			}
			if (i != 0 && i % 10_0000 == 0) {
				System.out.println(i);
			}
		}
		explorer.close();
	}
}
