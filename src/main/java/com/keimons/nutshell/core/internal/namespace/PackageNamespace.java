package com.keimons.nutshell.core.internal.namespace;

import com.keimons.nutshell.core.internal.utils.NClassUtils;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class PackageNamespace extends DefaultNamespace {

	private final String packageName;

	public PackageNamespace(ClassLoader classLoader, String packageName) {
		super(classLoader, loadClass(classLoader, packageName));
		this.packageName = packageName;
	}

	private static Map<String, Class<?>> loadClass(ClassLoader classLoader, String packageName) {
		Set<Class<?>> classes = NClassUtils.findClasses(classLoader, packageName, true);
		return classes.stream().collect(Collectors.toMap(Class::getName, cls -> cls));
	}

	public String getPackageName() {
		return packageName;
	}
}
