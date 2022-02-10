package com.keimons.nutshell.core.bootstrap;

import com.keimons.nutshell.core.ApplicationContext;
import com.keimons.nutshell.core.assembly.Assembly;

import java.util.List;

/**
 * 链接
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class AutolinkBootstrap implements Bootstrap {

	@Override
	public void install(ApplicationContext context, Assembly assembly) throws Throwable {
		assembly.linkInstalls();
	}

	@Override
	public void hotswap(ApplicationContext context, List<Assembly> inbounds, List<Assembly> outbounds) throws Throwable {
		for (Assembly assembly : inbounds) {
			assembly.linkInstalls();
			assembly.linkUpgrades();
			outbounds.add(assembly);
		}
	}
}
