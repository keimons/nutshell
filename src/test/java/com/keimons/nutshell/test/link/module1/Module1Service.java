package com.keimons.nutshell.test.link.module1;

import com.keimons.nutshell.core.Autolink;
import com.keimons.nutshell.test.link.module2.Module2Sharable;

/**
 * 模拟模块1
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
public class Module1Service implements Module1Sharable {

	@Autolink
	public Module2Sharable sharable;

	@Override
	public String name() {
		return sharable.name();
	}
}
