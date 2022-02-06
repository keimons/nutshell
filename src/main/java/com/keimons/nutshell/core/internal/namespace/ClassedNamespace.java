package com.keimons.nutshell.core.internal.namespace;

import com.keimons.nutshell.core.internal.utils.CleanerUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassedNamespace extends DefaultNamespace {

	public ClassedNamespace(ClassLoader classLoader, List<String> classNames) throws ClassNotFoundException {
		super(classLoader, loadClass(classLoader, classNames));
	}

	private static Map<String, Class<?>> loadClass(ClassLoader classLoader, List<String> classNames) throws ClassNotFoundException {
		Map<String, Class<?>> classes = new HashMap<>(classNames.size());
		for (String className : classNames) {
			Class<?> clazz = classLoader.loadClass(className);
			classes.put(className, clazz);
			CleanerUtils.register(clazz);
		}
		return classes;
	}
}
