package com.keimons.nutshell.core.bootstrap;

import com.keimons.nutshell.core.ApplicationContext;
import com.keimons.nutshell.core.assembly.Assembly;
import com.keimons.nutshell.core.internal.utils.ThrowableUtils;

import java.util.List;

/**
 * 切出分支
 * <p>
 * 切出一个分支以准备更新。为防止失败，所有的操作都在切出的分支上。引导失败时，需要回滚状态。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class ForkBootstrap implements Bootstrap {

	@Override
	public void install(ApplicationContext context, Assembly assembly) throws Throwable {
		// do nothing
	}

	@Override
	public void hotswap(ApplicationContext context, List<Assembly> inbounds, List<Assembly> outbounds) throws Throwable {
		inbounds.stream().filter(ThrowableUtils.wrapper(Assembly::fork)).forEach(outbounds::add);
	}
}
