package com.keimons.nutshell.explorer.test.explorer;

import com.keimons.nutshell.explorer.ConsumerTask;
import com.keimons.nutshell.explorer.support.ReorderedExplorer;
import org.junit.jupiter.api.Test;

/**
 * {@link ReorderedExplorer}关闭测试
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
public class ReorderedExplorerShutdownTest {

	@Test
	public void test() {
		ReorderedExplorer explorer = new ReorderedExplorer(4);

		explorer.shutdown(new ConsumerTask<>((tasks) -> {
			for (Runnable task : tasks) {
				task.run();
			}
		}));
	}
}
