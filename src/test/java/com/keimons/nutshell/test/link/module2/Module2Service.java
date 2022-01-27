package com.keimons.nutshell.test.link.module2;

/**
 * Module2Service
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 9
 **/
public class Module2Service implements Module2Sharable {

	@Override
	public String name() {
		System.out.println("call module2 service name");
		return "module2";
	}
}
