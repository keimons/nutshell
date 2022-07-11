package com.keimons.nutshell.test.link.module_a;

import com.keimons.nutshell.core.Autolink;
import com.keimons.nutshell.test.link.module_b.ModuleBSharable;

import java.util.Random;

/**
 * 模拟模块1
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 **/
public class ModuleAService implements ModuleASharable {

	@Autolink
	public ModuleBSharable sharable;

	Random random = new Random();

	@Override
	public String name() {
//		test();
		try {
			Thread.sleep(1000L + random.nextInt(1));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Module A");
		return sharable.name();
	}

//	public void test() {
//		System.out.println("Module A");
//	}
}
