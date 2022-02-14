package com.keimons.nutshell.core.internal;

import com.keimons.nutshell.core.ApplicationContext;
import com.keimons.nutshell.core.assembly.Assembly;

import java.util.HashMap;
import java.util.Map;

/**
 * 分支{@link ApplicationContext}
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class ForkApplicationContext implements ApplicationContext {

	private final ApplicationContext context;

	private final Map<String, Assembly> _assemblies = new HashMap<String, Assembly>();

	private final Map<String, Assembly> _implements = new HashMap<String, Assembly>();

	public ForkApplicationContext(ApplicationContext context) {
		this.context = context;
		_assemblies.putAll(context.getAssemblies());
		_implements.putAll(context.getImplements());
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

	@Override
	public ApplicationContext fork() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void join(ApplicationContext context) {
		throw new UnsupportedOperationException();
	}
}
