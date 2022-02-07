package com.keimons.nutshell.test.link;

import com.keimons.nutshell.core.Autolink;
import com.keimons.nutshell.core.NutshellApplication;
import com.keimons.nutshell.core.NutshellLauncher;
import com.keimons.nutshell.test.link.module1.Module1Sharable;
import org.junit.jupiter.api.Test;

/**
 * 测试
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
public class AutolinkTest {

	@Autolink
	public Module1Sharable sharable;

	@Test
	public void test() throws Throwable {
		NutshellApplication application = NutshellLauncher.run(this);
		System.out.println(sharable.name());
		application.update("com.keimons.nutshell.test.link.module1");
		System.out.println(sharable.name());
		System.gc();
	}
}
