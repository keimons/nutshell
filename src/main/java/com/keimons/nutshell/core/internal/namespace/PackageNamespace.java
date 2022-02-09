package com.keimons.nutshell.core.internal.namespace;

import com.keimons.nutshell.core.internal.utils.ClassUtils;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PackageNamespace extends DefaultNamespace {

	private final String root;

	private final String subpackage;

	public PackageNamespace(ClassLoader classLoader, String root, String subpackage) {
		super(classLoader, loadClass(classLoader, subpackage));
		this.root = root;
		this.subpackage = subpackage;
	}

	private static Map<String, Class<?>> loadClass(ClassLoader classLoader, String packageName) {
		Set<Class<?>> classes = ClassUtils.findClasses(classLoader, packageName, true);
		return classes.stream().collect(Collectors.toMap(Class::getName, cls -> cls));
	}

	public String getRoot() {
		return root;
	}

	public String getSubpackage() {
		return subpackage;
	}
}
