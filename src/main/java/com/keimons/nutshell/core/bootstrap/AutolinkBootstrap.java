package com.keimons.nutshell.core.bootstrap;

import com.keimons.nutshell.core.ApplicationContext;
import com.keimons.nutshell.core.assembly.Assembly;

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
	public void hotswap(ApplicationContext context, Assembly assembly) throws Throwable {
		assembly.linkInstalls();
		assembly.linkUpgrades();
	}
}
