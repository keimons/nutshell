package com.keimons.nutshell.core.assembly;

import com.keimons.nutshell.core.Autolink;
import com.keimons.nutshell.core.Context;
import com.keimons.nutshell.core.NutshellClassLoader;
import com.keimons.nutshell.core.inject.Injectors;
import com.keimons.nutshell.core.internal.InternalClassUtils;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * 程序集
 * <p>
 * assembly的划分是一件困难而又复杂的事。目前支持的划分方案：
 * <ul>
 *     <li>指定所有文件：直接指定这个模块中的所有文件。</li>
 *     <li>按照目录划分：直接指定一个目录为根目录，根目录下每个包为一个模块。</li>
 *     <li>按照模块划分：按照每一个模块视为一个插件。</li>
 * </ul>
 * 推荐的目录结构：
 * <pre>
 *     com
 *       +--module1
 *       |    +--&#64;Assemble
 *       |    +--Service1
 *       |    +--Manager1
 *       |    +--module2
 *       |         +--&#64;Assemble
 *       |         +--Service2
 *       |         +--Manager2
 *       +--module3
 *       |    +--&#64;Assemble
 *       |    +--Service3
 *       |    +--Manager3
 *       +--moduleX
 * </pre>
 * <p>
 * 程序集中记录了所有的类文件，这并不是一成不变的，模块更新时，有可能增删模块
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 9
 **/
public class Assembly {

	/**
	 * 名称
	 * <p>
	 * 名称是{@code Assembly}的唯一标识。在整个生存周期中，只有名称是一定不变的，
	 * 依赖名称查找{@code Assembly}。
	 * <p>
	 * 注意：应用程序中禁止出现两个同名的{@code Assembly}。
	 */
	private final String name;

	/**
	 * 已装载的{@code Assembly}
	 */
	private AbstractInnerAssembly assembly;

	/**
	 * 监听器
	 */
	private List<Listener> installs = new ArrayList<>();
	private List<Listener> upgrades = new ArrayList<>();

	private int version;

	public Assembly(String name, AbstractInnerAssembly assembly) {
		this.name = name;
		this.assembly = assembly;
	}

	public int version() {
		return version;
	}

	public void addListener(Listener.ListenerType type, Listener listener) {
		if (type == Listener.ListenerType.INSTALL) {
			installs.add(listener);
		} else {
			upgrades.add(listener);
		}
	}

	public Collection<Class<?>> getClasses() {
		return assembly.getClasses();
	}

	public Class<?> getClass(String className) {
		return assembly.getClass(className);
	}

	public Set<Class<?>> findInjections(Class<? extends Annotation> annotation) {
		return assembly.findInjections(annotation);
	}

	public Object getInstance(String interfaceName) {
		return assembly.getInstance(interfaceName);
	}

	public void registerInstance(String interfaceName, Object instance) {
		assembly.registerInstance(interfaceName, instance);
	}

	public void inject(Context context) throws Throwable {
		Collection<Object> instances = assembly.getInstances();
		for (Object instance : instances) {
			if (InternalClassUtils.hasAnnotation(instance.getClass(), Autolink.class)) {
				Injectors injectors = Injectors.of(instance.getClass(), Autolink.class);
				injectors.inject(context, this, instance);
				System.out.println("inject instance: " + instance.getClass());
			}
		}
	}

	public void reset(String... classNames) {
		installs.clear();
		assembly = new ClassedInnerAssembly(new NutshellClassLoader(name, classNames), List.of(classNames));
	}

	/**
	 * 获取{@code Assembly}名称
	 *
	 * @return {@code Assembly}名称
	 */
	public String getName() {
		return name;
	}

	public static Assembly root(Object root) {
		return new Assembly("root", new RootInnerAssembly(root));
	}

	public static Assembly of(String name, String... classNames) {
		NutshellClassLoader loader = new NutshellClassLoader(name, classNames);
		return of(name, loader, List.of(classNames));
	}

	static Assembly of(String name, ClassLoader loader, List<String> classNames) {
		ClassedInnerAssembly inner = new ClassedInnerAssembly(loader, classNames);
		return new Assembly(name, inner);
	}

	public void linkInstalls() throws Throwable {
		for (Listener install : installs) {
			install.apply();
		}
	}

	public void linkUpgrades() throws Throwable {
		for (Listener install : upgrades) {
			install.apply();
		}
	}

	private static abstract class AbstractInnerAssembly {

		abstract List<String> getClassNames();

		public abstract Collection<Class<?>> getClasses();

		public abstract Class<?> getClass(String className);

		public abstract Set<Class<?>> findInjections(Class<? extends Annotation> annotation);

		public abstract Collection<Object> getInstances();

		public abstract Object getInstance(String interfaceName);

		public abstract void registerInstance(String interfaceName, Object instance);
	}

	private static class RootInnerAssembly extends AbstractInnerAssembly {

		Object root;

		public RootInnerAssembly(Object root) {
			this.root = root;
		}

		@Override
		List<String> getClassNames() {
			return Collections.singletonList(root.getClass().getName());
		}

		@Override
		public Collection<Class<?>> getClasses() {
			return Collections.singleton(root.getClass());
		}

		@Override
		public Class<?> getClass(String className) {
			if (root.getClass().getName().equals(className)) {
				return root.getClass();
			}
			return null;
		}

		@Override
		public Set<Class<?>> findInjections(Class<? extends Annotation> annotation) {
			return InternalClassUtils.findInjections(Collections.singleton(root.getClass()), annotation);
		}

		@Override
		public Collection<Object> getInstances() {
			return Collections.singleton(root);
		}

		@Override
		public Object getInstance(String interfaceName) {
			return null;
		}

		@Override
		public void registerInstance(String interfaceName, Object instance) {
			throw new UnsupportedOperationException();
		}
	}

	private static class ClassedInnerAssembly extends AbstractInnerAssembly {

		protected List<String> classNames;

		Map<String, Class<?>> classes;

		protected ClassLoader loader;

		private Set<Class<?>> injections;

		private Map<String, Object> instances = new HashMap<>();

		public ClassedInnerAssembly(ClassLoader loader, List<String> classNames) {
			this.loader = loader;
			this.classNames = classNames;
			this.classes = new HashMap<>(classNames.size());
			for (String className : classNames) {
				try {
					classes.put(className, loader.loadClass(className));
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		List<String> getClassNames() {
			return classNames;
		}

		@Override
		public Collection<Class<?>> getClasses() {
			return classes.values();
		}

		@Override
		public Class<?> getClass(String className) {
			return classes.get(className);
		}

		@Override
		public Set<Class<?>> findInjections(Class<? extends Annotation> annotation) {
			if (injections == null) {
				injections = InternalClassUtils.findInjections(classes.values(), annotation);
			}
			return injections;
		}

		@Override
		public Collection<Object> getInstances() {
			return instances.values();
		}

		@Override
		public Object getInstance(String interfaceName) {
			return instances.get(interfaceName);
		}

		@Override
		public void registerInstance(String interfaceName, Object instance) {
			instances.put(interfaceName, instance);
		}
	}

	@Override
	public String toString() {
		return "assembly: name = '" + name + "', version = " + version;
	}
}
