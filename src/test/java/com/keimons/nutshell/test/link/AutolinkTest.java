package com.keimons.nutshell.test.link;

import com.keimons.nutshell.core.Autolink;
import com.keimons.nutshell.test.Launcher;
import com.keimons.nutshell.test.link.module_a.ModuleASharable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * 测试
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
@ExtendWith(Launcher.class)
public class AutolinkTest {

	@Autolink
	public ModuleASharable sharable;

	@Test
	public void test() throws Throwable {
		while (true) {
			System.out.println(sharable.name());
			Thread.sleep(2000);
		}
	}
}
