package com.keimons.nutshell.core.bootstrap;

import com.keimons.nutshell.core.ApplicationContext;
import com.keimons.nutshell.core.Autolink;
import com.keimons.nutshell.core.assembly.Assembly;
import com.keimons.nutshell.core.internal.utils.ClassUtils;
import com.keimons.nutshell.core.internal.utils.ConsumerUtils;

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
				.map(Class::getName).forEach(consumer(context, assembly))
		;
	}

	@Override
	public void hotswap(ApplicationContext context, Assembly assembly) throws Throwable {
		install(context, assembly);
	}

	private Consumer<String> consumer(ApplicationContext context, Assembly assembly) throws Throwable {
		return ConsumerUtils.wrapper(injectName -> {
			Map<String, Class<?>> classes = assembly.getClasses();
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
			assembly.registerInstance(injectName, instance);
			context.getInstances().put(injectName, assembly);
		});
	}
}
