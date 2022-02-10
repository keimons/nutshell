package com.keimons.nutshell.test.link.module_a;

import com.keimons.nutshell.core.Autolink;
import com.keimons.nutshell.test.link.module_b.Module2Sharable;

/**
 * 模拟模块1
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
public class ModuleAService implements ModuleASharable {

	@Autolink
	public Module2Sharable sharable;

	@Override
	public String name() {
//		test();
		return sharable.name();
	}

//	public void test() {
//		System.out.println("Module A");
//	}
}
