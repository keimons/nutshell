package com.keimons.nutshell.test.safepoint.module_b;

import java.util.Random;

/**
 * 模拟模块2
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
public class ModuleBService implements ModuleBSharable {

	Random random = new Random();

	@Override
	public String name() {
		try {
			Thread.sleep(1000L + random.nextInt(2000));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return "[" + Thread.currentThread().getName() + "] Module B";
	}
}
