package com.keimons.nutshell.core.internal;

import com.keimons.nutshell.core.NutshellApplication;
import com.keimons.nutshell.core.assembly.Assembly;
import com.keimons.nutshell.core.internal.utils.FileUtils;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;

/**
 * 热插拔类装载
 * <p>
 * 每个类装载器都有装载范围，对于超出装载范围的类，使用父类装载器装载。
 * nutshell的热插拔依赖于类装载的装载范围。
 * nutshell定义启动类{@link NutshellApplication}所在类为根目录，
 * 根目录下，每个子目录为一个{@link Assembly}并拥有单独的类装载。
 * 热插拔类装载器无法装载根目录类，只能用于装载子目录类。
 * <p>
 * 如果类中有依赖类，那么根据依赖类位置，如果依赖类位于根目录下，
 * 则使用{@link HotswapClassLoader}装载，如果依赖类不在根目录下，
 * 则使用父类装载器装载。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
public class HotswapClassLoader extends URLClassLoader {

	/**
	 * 装载策略
	 * <p>
	 * 每个类装载器都有装载策略，装载策略决定了哪些类可以使用这个类装载器装载。
	 */
	private final LoadStrategy strategy;

	/**
	 * 父类装载器
	 */
	private final ClassLoader parent;

	/**
	 * 构造方法
	 *
	 * @param name 类装载器的名字
	 */
	public HotswapClassLoader(String name, ClassLoader parent, String... classNames) {
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
	public HotswapClassLoader(String name, ClassLoader parent, String pkg) {
		super(name, new URL[]{}, null);
		this.parent = parent;
		this.strategy = new PackagePolicy(pkg);
	}

	/**
	 * 装载类
	 *
	 * @param className 类名
	 * @param bytes     类文件
	 * @return 装载的类
	 */
	public Class<?> loadClass(String className, byte[] bytes) {
		Class<?> clazz = findLoadedClass(className);
		if (clazz == null) {
			clazz = defineClass(className, bytes, 0, bytes.length);
			MemoryMonitor.register(clazz);
		}
		return clazz;
	}

	@Override
	protected Class<?> findClass(String className) throws ClassNotFoundException {
		if (strategy.test(className)) {
			try {
				byte[] bytes = FileUtils.readClass(className);
				return loadClass(className, bytes);
			} catch (Exception e) {
				throw new ClassNotFoundException(className, e);
			}
		} else {
			return Class.forName(className, true, parent);
		}
	}

	/**
	 * 获取父类装载
	 *
	 * @return 父类装载
	 */
	public ClassLoader getParentClassLoader() {
		return parent;
	}

	@Override
	public String toString() {
		return "HotswapClassLoader(" + getName() + ')';
	}

	/**
	 * 装载策略
	 */
	private interface LoadStrategy {

		/**
		 * 测试类能否使用这个类装载器装载
		 *
		 * @param className 类名
		 * @return true.可以使用 false.不能使用
		 */
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
