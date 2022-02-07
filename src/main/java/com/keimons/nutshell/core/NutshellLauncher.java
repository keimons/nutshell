package com.keimons.nutshell.core;

import com.keimons.nutshell.core.assembly.Assembly;
import com.keimons.nutshell.core.internal.utils.PackageUtils;

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

	public static NutshellApplication run(Class<?> clazz, String... args) {
		return null;
	}

	public static NutshellApplication run(Object object, String... args) throws Throwable {
		NutshellApplication application = new NutshellApplication();
		application.setup(defaultAssemblies(object));
		return application;
	}

	private static List<Assembly> defaultAssemblies(Object object) {
		List<Assembly> assemblies = new ArrayList<Assembly>();
		assemblies.add(Assembly.of(object));
		String packageName = object.getClass().getPackageName();
		Set<String> subpackages = PackageUtils.findSubpackages(packageName);
		for (String subpackage : subpackages) {
			assemblies.add(Assembly.of(object.getClass().getClassLoader(), subpackage));
		}
		return assemblies;
	}
}
