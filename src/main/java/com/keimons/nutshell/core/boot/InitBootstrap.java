package com.keimons.nutshell.core.boot;

import com.keimons.nutshell.core.Autolink;
import com.keimons.nutshell.core.Context;
import com.keimons.nutshell.core.assembly.Assembly;
import com.keimons.nutshell.core.internal.ConsumerUtils;
import com.keimons.nutshell.core.internal.InternalClassUtils;

import java.util.function.Consumer;

/**
 * 初始化
 * <p>
 * 安装程序的第一步，将正在安装的模块进行缓存。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 9
 **/
public class InitBootstrap implements Bootstrap {

	@Override
	public void invoke(Context context, Mode mode, Assembly assembly) throws Throwable {
		context.getAssemblies().values().stream()
				.flatMap(item -> item.findInjections(Autolink.class).stream())
				.map(Class::getName).forEach(consumer(context, assembly))
		;
	}

	private Consumer<String> consumer(Context context, Assembly assembly) throws Throwable {
		return ConsumerUtils.wrapper(injectName -> {
			Class<?> injectType = assembly.getClass(injectName);
			if (injectType == null) {
				return;
			}
			// find inject type's implement in assembly.
			Class<?> implement = InternalClassUtils.findImplement(assembly.getClasses(), injectType);
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
