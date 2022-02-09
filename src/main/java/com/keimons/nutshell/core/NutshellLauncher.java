package com.keimons.nutshell.core;

import com.keimons.nutshell.core.assembly.Assembly;
import com.keimons.nutshell.core.internal.utils.PackageUtils;
import com.keimons.nutshell.core.monitor.HotswapObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 启动器
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
public class NutshellLauncher {

	private static final ThreadLocal<NutshellApplication> LOCAL = new ThreadLocal<NutshellApplication>();

	public static NutshellApplication run(Class<?> clazz, String... args) {
		return null;
	}

	public static NutshellApplication run(Object object, HotswapObserver<?> observer, String... args) throws Throwable {
		NutshellApplication application = new NutshellApplication(object, observer);
		application.install(defaultAssemblies(object));
		LOCAL.set(application);
		return application;
	}

	public static NutshellApplication getApplication() {
		return LOCAL.get();
	}

	private static List<Assembly> defaultAssemblies(Object object) {
		List<Assembly> assemblies = new ArrayList<Assembly>();
		assemblies.add(Assembly.of(object));
		String packageName = object.getClass().getPackageName();
		Set<String> subpackages = PackageUtils.findSubpackages(packageName);
		for (String subpackage : subpackages) {
			assemblies.add(Assembly.of(object.getClass().getClassLoader(), packageName, subpackage));
		}
		return assemblies;
	}
}
