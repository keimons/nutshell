package com.keimons.nutshell.explorer.test.explorer;

import com.keimons.nutshell.explorer.ConsumerTask;
import com.keimons.nutshell.explorer.support.ReorderExplorer;
import org.junit.jupiter.api.Test;

/**
 * {@link ReorderExplorer}关闭测试
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 **/
public class ReorderExplorerShutdownTest {

	@Test
	public void test() {
		ReorderExplorer explorer = new ReorderExplorer(4);

		explorer.shutdown(new ConsumerTask<>((tasks) -> {
			for (Runnable task : tasks) {
				task.run();
			}
		}));
	}
}
