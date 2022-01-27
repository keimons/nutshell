package com.keimons.nutshell.core;

import com.keimons.nutshell.core.assembly.Assembly;

/**
 * App
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 9
 **/
public class App {

	private static final App instance = new App();

	public static App getInstance() {
		return instance;
	}

	public AssemblyContext context = new AssemblyContext();

	public void init(Assembly... assemblies) throws Throwable {
		for (Assembly assembly : assemblies) {
			context.add(assembly);
		}
		for (Assembly assembly : assemblies) {
			context.init(assembly);
		}
		for (Assembly assembly : assemblies) {
			context.install(assembly);
		}
		for (Assembly assembly : assemblies) {
			context.link(assembly);
		}
	}

	public void shutdown() {

	}
}
