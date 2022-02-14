package com.keimons.nutshell.core.internal;

import com.keimons.nutshell.core.ApplicationContext;
import com.keimons.nutshell.core.assembly.Assembly;

import java.util.HashMap;
import java.util.Map;

/**
 * 程序集上下文环境
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
public class DefaultApplicationContext implements ApplicationContext {

	private Map<String, Assembly> _assemblies = new HashMap<String, Assembly>();

	private Map<String, Assembly> _implements = new HashMap<String, Assembly>();

	@Override
	public ApplicationContext fork() {
		return new ForkApplicationContext(this);
	}

	@Override
	public void add(Assembly assembly) {
		_assemblies.put(assembly.getName(), assembly);
	}

	@Override
	public Assembly findAssembly(String assemblyName) {
		return _assemblies.get(assemblyName);
	}

	@Override
	public Map<String, Assembly> getAssemblies() {
		return _assemblies;
	}

	@Override
	public Assembly findImplement(String interfaceName) {
		return _implements.get(interfaceName);
	}

	@Override
	public Map<String, Assembly> getImplements() {
		return _implements;
	}
}
