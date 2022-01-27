package com.keimons.nutshell.core.boot;

import com.keimons.nutshell.core.Context;
import com.keimons.nutshell.core.assembly.Assembly;

/**
 * 链接
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 9
 */
public class LinkBootstrap implements Bootstrap {

	@Override
	public void invoke(Context context, Mode mode, Assembly assembly) throws Throwable {
		assembly.linkInstalls();
		if (mode == Mode.UPGRADE) {
			assembly.linkUpgrades();
		}
	}
}
