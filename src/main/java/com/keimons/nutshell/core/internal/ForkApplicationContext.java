package com.keimons.nutshell.core.internal;

import com.keimons.nutshell.core.ApplicationContext;
import com.keimons.nutshell.core.assembly.Assembly;

import java.util.Map;

/**
 * 分支{@link ApplicationContext}
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class ForkApplicationContext implements ApplicationContext {

	private ApplicationContext context;

	public ForkApplicationContext(ApplicationContext context) {
		this.context = context;
	}

	@Override
	public ApplicationContext fork() {
		return this;
	}

	@Override
	public void add(Assembly assembly) {

	}

	@Override
	public Assembly findAssembly(String assemblyName) {
		return null;
	}

	@Override
	public Map<String, Assembly> getAssemblies() {
		return null;
	}

	@Override
	public Assembly findImplement(String interfaceName) {
		return null;
	}

	@Override
	public Map<String, Assembly> getImplements() {
		return null;
	}
}
