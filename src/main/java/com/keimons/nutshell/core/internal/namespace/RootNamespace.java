package com.keimons.nutshell.core.internal.namespace;

import com.keimons.nutshell.core.Autolink;
import com.keimons.nutshell.core.assembly.Assembly;
import com.keimons.nutshell.core.internal.utils.ClassUtils;

import java.util.Set;

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
public class RootNamespace extends AbstractNamespace {

	protected final Object root;

	public RootNamespace(Object root) throws ClassNotFoundException {
		super(root.getClass().getClassLoader());
		this.root = root;
		this.exports.put(Assembly.ROOT, root);

		String packageName = root.getClass().getPackageName();
		Set<String> classNames = ClassUtils.findClasses(packageName, false);
		ClassLoader classLoader = root.getClass().getClassLoader();
		for (String className : classNames) {
			Class<?> clazz = classLoader.loadClass(className);
			this.classes.put(className, clazz);
		}
	}
}
