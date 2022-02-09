package com.keimons.nutshell.core;

import com.keimons.nutshell.core.internal.utils.CleanerUtils;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;

/**
 * ClassLoader
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
public class NutshellClassLoader extends URLClassLoader {

	private final LoadStrategy strategy;

	private final ClassLoader parent;

	/**
	 * 构造方法
	 *
	 * @param name 类装载器的名字
	 */
	public NutshellClassLoader(String name, ClassLoader parent, String... classNames) {
		super(name, new URL[]{}, null);
		this.parent = parent;
		this.strategy = new ClassNamesPolicy(Set.of(classNames));
	}

	/**
	 * 构造方法
	 *
	 * @param parent 父类加载
	 * @param name   类装载器的名字
	 * @param pkg    类装载器的装载范围
	 */
	public NutshellClassLoader(String name, ClassLoader parent, String pkg) {
		super(name, new URL[]{}, null);
		this.parent = parent;
		this.strategy = new PackagePolicy(pkg);
	}

	@Override
	protected Class<?> findClass(String className) throws ClassNotFoundException {
		if (strategy.test(className)) {
			try {
				String fileName = "/" + className.replaceAll("\\.", "/") + ".class";
				InputStream is = getClass().getResourceAsStream(fileName);
				if (is == null) {
					throw new ClassNotFoundException(className);
				}
				byte[] b = new byte[is.available()];
				int read = is.read(b);
				if (b.length != read) {
					throw new ClassNotFoundException(className);
				}
				Class<?> clazz = defineClass(className, b, 0, b.length);
				CleanerUtils.register(clazz);
				return clazz;
			} catch (Exception e) {
				throw new ClassNotFoundException(className, e);
			}
		} else {
			return Class.forName(className, true, parent);
		}
	}

	public ClassLoader getParentClassLoader() {
		return parent;
	}

	private interface LoadStrategy {

		boolean test(String className);
	}

	private static class PackagePolicy implements LoadStrategy {

		private final String packageName;

		public PackagePolicy(String packageName) {
			this.packageName = packageName;
		}

		@Override
		public boolean test(String className) {
			return className.startsWith(packageName);
		}
	}

	private static class ClassNamesPolicy implements LoadStrategy {

		private final Set<String> classNames;

		public ClassNamesPolicy(Set<String> packageName) {
			this.classNames = packageName;
		}

		@Override
		public boolean test(String className) {
			return classNames.contains(className);
		}
	}
}
