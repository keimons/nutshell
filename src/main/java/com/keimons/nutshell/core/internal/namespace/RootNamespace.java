package com.keimons.nutshell.core.internal.namespace;

import com.keimons.nutshell.core.Autolink;
import com.keimons.nutshell.core.internal.utils.ClassUtils;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 根命名空间
 * <p>
 * 根命名空间是指启动类所在目录，该目录下所有类都是根命名空间的类，不包含子目录，
 * 根命名空间一旦被加载无法被卸载。得益于{@link Autolink}的存在，
 * 根命名空间中注入的对象是可以被更新的。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
public class RootNamespace extends DefaultNamespace {

	Object root;

	public RootNamespace(Object root) {
		super(root.getClass().getClassLoader(), loadClass(root.getClass().getClassLoader(), root.getClass().getPackageName()));
		this.root = root;
		this.exports.put("ROOT", root);
	}

	private static Map<String, Class<?>> loadClass(ClassLoader classLoader, String packageName) {
		Set<Class<?>> classes = ClassUtils.findClasses(classLoader, packageName, false);
		return classes.stream().collect(Collectors.toMap(Class::getName, clazz -> clazz));
	}
}
