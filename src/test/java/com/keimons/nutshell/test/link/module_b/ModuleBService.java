package com.keimons.nutshell.test.link.module_b;

/**
 * 模拟模块2
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
public class ModuleBService implements Module2Sharable {

	@Override
	public String name() {
		return "Module B";
	}
}
