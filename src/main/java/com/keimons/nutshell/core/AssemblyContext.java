package com.keimons.nutshell.core;

import com.keimons.nutshell.core.assembly.Assembly;
import com.keimons.nutshell.core.internal.InternalClassUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 程序集上下文环境
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 9
 **/
public class AssemblyContext implements Context {

	private Map<String, Assembly> assemblies = new HashMap<String, Assembly>();

	private Map<String, Assembly> instances = new HashMap<String, Assembly>();

	@Override
	public void add(Assembly assembly) {
		assemblies.put(assembly.getName(), assembly);
	}

	@Override
	public Assembly get(String name) {
		return assemblies.get(name);
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
	public void init(Assembly assembly) throws Throwable {
		Collection<Class<?>> classes = assembly.getClasses();
		for (Assembly value : assemblies.values()) {
			Set<Class<?>> injections = value.findInjections(Autolink.class);
			for (Class<?> injection : injections) {
				String className = injection.getName();
				Class<?> injectType = assembly.getClass(className);
				if (injectType == null) {
					continue;
				}
				// find inject type's implement in assembly.
				Class<?> implement = InternalClassUtils.findImplement(classes, injectType);
				if (implement == null) {
					// ignore
					continue;
				}
				Object instance = implement.getConstructor().newInstance();
				System.out.println("instance class: " + implement.getName());
				assembly.registerInstance(className, instance);
				instances.put(className, assembly);
			}
		}
	}

	@Override
	public void install(Assembly assembly) throws Throwable {
		assembly.inject(this);
	}

	@Override
	public void link(Assembly assembly) throws Throwable {
		assembly.linkInstalls();
		assembly.linkUpgrades();
	}

	@Override
	public Assembly findInstance(String className) {
		return instances.get(className);
	}
}
