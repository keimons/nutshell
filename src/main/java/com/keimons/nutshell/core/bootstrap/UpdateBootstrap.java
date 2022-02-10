package com.keimons.nutshell.core.bootstrap;

import com.keimons.nutshell.core.ApplicationContext;
import com.keimons.nutshell.core.assembly.Assembly;

import java.util.List;

public class UpdateBootstrap implements Bootstrap {

	@Override
	public void install(ApplicationContext context, Assembly assembly) throws Throwable {
		// do nothing
	}

	@Override
	public void hotswap(ApplicationContext context, List<Assembly> inbounds, List<Assembly> outbounds) throws Throwable {
		for (Assembly assembly : inbounds) {
			if (assembly.reset()) {
				outbounds.add(assembly);
			}
		}
	}
}
