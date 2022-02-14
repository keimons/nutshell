package com.keimons.nutshell.core.bootstrap;

import com.keimons.nutshell.core.ApplicationContext;
import com.keimons.nutshell.core.Autolink;
import com.keimons.nutshell.core.assembly.Assembly;
import com.keimons.nutshell.core.internal.utils.ClassUtils;
import com.keimons.nutshell.core.internal.utils.ThrowableUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 初始化
 * <p>
 * 安装程序的第一步，将正在安装的模块进行缓存。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
public class InitBootstrap implements Bootstrap {

	@Override
	public void install(ApplicationContext context, Assembly assembly) throws Throwable {
		context.getAssemblies().values().stream()
				.flatMap(item -> item.findInjections(Autolink.class).stream())
				.map(Class::getName).forEach(consumer(context, Mode.INSTALL, assembly))
		;
	}

	@Override
	public void hotswap(ApplicationContext context, List<Assembly> inbounds, List<Assembly> outbounds) throws Throwable {
		for (Assembly assembly : inbounds) {
			context.getAssemblies().values().stream()
					.flatMap(item -> item.findInjections(Autolink.class).stream())
					.map(Class::getName).forEach(consumer(context, Mode.HOTSWAP, assembly))
			;
			outbounds.add(assembly);
		}
	}

	private Consumer<String> consumer(ApplicationContext context, Mode mode, Assembly assembly) throws Throwable {
		return ThrowableUtils.wrapper(injectName -> {
			Map<String, Class<?>> classes = assembly.getClasses(mode);
			Class<?> injectType = classes.get(injectName);
			if (injectType == null) {
				return;
			}
			// find inject type's implement in assembly.
			Class<?> implement = ClassUtils.findImplement(classes.values(), injectType);
			if (implement == null) {
				// ignore
				return;
			}
			Object instance = implement.getConstructor().newInstance();
			System.out.println("instance class: " + implement.getName());
			assembly.registerImplement(mode, injectName, instance);
			context.getImplements().put(injectName, assembly);
		});
	}
}
