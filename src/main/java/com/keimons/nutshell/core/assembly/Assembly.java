package com.keimons.nutshell.core.assembly;

import com.keimons.nutshell.core.ApplicationContext;
import com.keimons.nutshell.core.Autolink;
import com.keimons.nutshell.core.NutshellClassLoader;
import com.keimons.nutshell.core.inject.Injectors;
import com.keimons.nutshell.core.internal.namespace.DefaultNamespace;
import com.keimons.nutshell.core.internal.namespace.Namespace;
import com.keimons.nutshell.core.internal.namespace.PackageNamespace;
import com.keimons.nutshell.core.internal.utils.ClassUtils;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
 * @since 11
 **/
public class Assembly {

	/**
	 * 根
	 */
	public static final String ROOT = "ROOT";

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
	private Namespace namespace;

	/**
	 * 监听器
	 */
	private List<Listener> installs = new ArrayList<>();
	private List<Listener> hotswaps = new ArrayList<>();

	private int version;

	public Assembly(String name, Namespace namespace) {
		this.name = name;
		this.namespace = namespace;
	}

	public int version() {
		return version;
	}

	public void addListener(Listener.ListenerType type, Listener listener) {
		if (type == Listener.ListenerType.INSTALL) {
			installs.add(listener);
		} else {
			hotswaps.add(listener);
		}
	}

	public Map<String, Class<?>> getClasses() {
		return namespace.getClasses();
	}

	public Set<Class<?>> findInjections(Class<? extends Annotation> annotation) {
		return ClassUtils.findInjections(namespace.getClasses().values(), annotation);
	}

	public Object getInstance(String interfaceName) {
		Map<String, Object> exports = namespace.getExports();
		return exports.get(interfaceName);
	}

	public void registerInstance(String interfaceName, Object instance) {
		namespace.getExports().put(interfaceName, instance);
	}

	public void inject(ApplicationContext context) throws Throwable {
		Map<String, Object> instances = namespace.getExports();
		for (Object instance : instances.values()) {
			if (ClassUtils.hasAnnotation(instance.getClass(), Autolink.class)) {
				Injectors injectors = Injectors.of(instance.getClass(), Autolink.class);
				injectors.inject(context, this, instance);
				System.out.println("inject instance: " + instance.getClass());
			}
		}
	}

	public void reset() {
		if (this.namespace instanceof PackageNamespace) {
			installs.clear();
			PackageNamespace namespace = (PackageNamespace) this.namespace;
			ClassLoader classLoader = this.namespace.getClassLoader();
			ClassLoader parent = classLoader instanceof NutshellClassLoader ? ((NutshellClassLoader) classLoader).getParentClassLoader() : classLoader.getParent();
			this.namespace = new PackageNamespace(new NutshellClassLoader(name, parent, namespace.getRoot()), namespace.getRoot(), namespace.getSubpackage());
		}
	}

	/**
	 * 获取{@code Assembly}名称
	 *
	 * @return {@code Assembly}名称
	 */
	public String getName() {
		return name;
	}

	public static Assembly of(Object object) {
		ClassLoader classLoader = object.getClass().getClassLoader();
		String packageName = object.getClass().getPackageName();
		Set<Class<?>> set = ClassUtils.findClasses(classLoader, packageName, false);
		Map<String, Class<?>> classes = set.stream().collect(Collectors.toMap(Class::getName, cls -> cls));
		Namespace namespace = new DefaultNamespace(classLoader, classes);
		namespace.getExports().put(ROOT, object);
		return new Assembly(ROOT, namespace);
	}

	/**
	 * 生成一个{@link Assembly}
	 *
	 * @param parent     父类装载器
	 * @param root       类装载器的根目录
	 * @param subpackage {@link Assembly}目录
	 * @return {@link Assembly}
	 */
	public static Assembly of(ClassLoader parent, String root, String subpackage) {
		NutshellClassLoader classLoader = new NutshellClassLoader(subpackage, parent, root);
		Namespace namespace = new PackageNamespace(classLoader, root, subpackage);
		return new Assembly(subpackage, namespace);
	}

	public void linkInstalls() throws Throwable {
		for (Listener install : installs) {
			install.apply();
		}
	}

	public void linkUpgrades() throws Throwable {
		for (Listener install : hotswaps) {
			install.apply();
		}
	}

	public Namespace getNamespace() {
		return namespace;
	}

	@Override
	public String toString() {
		return "assembly: name = '" + name + "', version = " + version;
	}
}
