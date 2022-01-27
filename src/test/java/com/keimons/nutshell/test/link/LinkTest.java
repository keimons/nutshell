package com.keimons.nutshell.test.link;

import com.keimons.nutshell.core.App;
import com.keimons.nutshell.core.Autolink;
import com.keimons.nutshell.core.assembly.Assembly;
import com.keimons.nutshell.test.link.module1.Module1Sharable;
import org.junit.jupiter.api.Test;

/**
 * Main
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 9
 **/
public class LinkTest {

	Assembly root = Assembly.root(this);

	String[] classNames1 = new String[]{
			"com.keimons.nutshell.test.link.module1.Module1Service",
			"com.keimons.nutshell.test.link.module1.Module1Sharable"
	};
	Assembly module1 = Assembly.of("module1", classNames1);

	String[] classNames2 = new String[]{
			"com.keimons.nutshell.test.link.module2.Module2Service",
			"com.keimons.nutshell.test.link.module2.Module2Sharable"
	};
	Assembly module2 = Assembly.of("module2", classNames2);

	{
		try {
			App.getInstance().init(root, module1, module2);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Autolink
	public Module1Sharable sharable;

	@Test
	public void test() throws Throwable {
		System.out.println(sharable.name());
		module1.reset(classNames1);
		App.getInstance().init(module1);
		System.out.println(sharable.name());
	}
}
