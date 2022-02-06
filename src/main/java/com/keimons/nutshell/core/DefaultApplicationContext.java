package com.keimons.nutshell.core;

import com.keimons.nutshell.core.assembly.Assembly;

import java.util.HashMap;
import java.util.Map;

/**
 * 程序集上下文环境
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 9
 **/
public class DefaultApplicationContext implements ApplicationContext {

	private Map<String, Assembly> assemblies = new HashMap<String, Assembly>();

	private Map<String, Assembly> instances = new HashMap<String, Assembly>();

	@Override
	public void add(Assembly assembly) {
		assemblies.put(assembly.getName(), assembly);
	}

	@Override
	public Assembly get(String packageName) {
		return assemblies.get(packageName);
	}

	@Override
	public Map<String, Assembly> getAssemblies() {
		return assemblies;
	}

	@Override
	public Map<String, Assembly> getInstances() {
		return instances;
	}

	@Override
	public Assembly findInstance(String className) {
		return instances.get(className);
	}
}
