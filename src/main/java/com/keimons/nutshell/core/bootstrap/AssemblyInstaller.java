package com.keimons.nutshell.core.bootstrap;

import com.keimons.nutshell.core.ApplicationContext;
import com.keimons.nutshell.core.assembly.Assembly;

import java.util.LinkedHashMap;
import java.util.List;

public class AssemblyInstaller {

	private LinkedHashMap<String, Bootstrap> bootstraps = new LinkedHashMap<String, Bootstrap>();

	public void addLast(String name, Bootstrap bootstrap) {
		bootstraps.put(name, bootstrap);
	}

	public void install(ApplicationContext context, Assembly... assemblies) throws Throwable {
		install(context, List.of(assemblies));
	}

	public void install(ApplicationContext context, List<Assembly> assemblies) throws Throwable {
		assemblies.forEach(context::add);
		for (Bootstrap bootstrap : bootstraps.values()) {
			for (Assembly assembly : assemblies) {
				bootstrap.setup(context, assembly);
			}
		}
	}

	public void update(ApplicationContext context, Assembly... assemblies) throws Throwable {
		update(context, List.of(assemblies));
	}

	public void update(ApplicationContext context, List<Assembly> assemblies) throws Throwable {
		assemblies.forEach(context::add);
		for (Bootstrap bootstrap : bootstraps.values()) {
			for (Assembly assembly : assemblies) {
				bootstrap.update(context, assembly);
			}
		}
	}
}
