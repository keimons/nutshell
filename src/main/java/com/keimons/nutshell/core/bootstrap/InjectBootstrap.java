package com.keimons.nutshell.core.bootstrap;

import com.keimons.nutshell.core.ApplicationContext;
import com.keimons.nutshell.core.assembly.Assembly;
import com.keimons.nutshell.core.assembly.AutolinkFactory;

import java.util.List;

/**
 * 注入
 * <p>
 * 注入的可能是真实的对象，也有可能是{@link AutolinkFactory}生成的自动链接。
 * 引导完成{@link Assembly}中注入点的注入。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class InjectBootstrap implements Bootstrap {

	@Override
	public void install(ApplicationContext context, Assembly assembly) throws Throwable {
		assembly.inject(context, Mode.INSTALL);
	}

	@Override
	public void hotswap(ApplicationContext context, List<Assembly> inbounds, List<Assembly> outbounds) throws Throwable {
		for (Assembly assembly : inbounds) {
			assembly.inject(context, Mode.HOTSWAP);
			outbounds.add(assembly);
		}
	}
}
