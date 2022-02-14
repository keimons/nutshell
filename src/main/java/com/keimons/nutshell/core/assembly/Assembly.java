package com.keimons.nutshell.core.assembly;

import com.keimons.nutshell.core.ApplicationContext;
import com.keimons.nutshell.core.Autolink;
import com.keimons.nutshell.core.bootstrap.Bootstrap;
import com.keimons.nutshell.core.inject.Injectors;
import com.keimons.nutshell.core.internal.HotswapClassLoader;
import com.keimons.nutshell.core.internal.namespace.Namespace;
import com.keimons.nutshell.core.internal.namespace.PackageNamespace;
import com.keimons.nutshell.core.internal.namespace.RootNamespace;
import com.keimons.nutshell.core.internal.utils.ClassUtils;
import com.keimons.nutshell.core.internal.utils.EqualsUtils;
import com.keimons.nutshell.core.internal.utils.FileUtils;
import com.keimons.nutshell.core.internal.utils.ThrowableUtils;

import java.io.IOException;
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
	 * 安装的{@code Assembly}
	 */
	private Namespace install;

	/**
	 * 更新的{@code Assembly}
	 */
	private Namespace hotswap;

	/**
	 * 模块引用监听器
	 */
	private List<Listener> listeners = new ArrayList<>();

	public Assembly(String name, Namespace namespace) {
		this.name = name;
		this.install = namespace;
	}

	public void addListener(Listener listener) {
		listeners.add(listener);
	}

	public Map<String, Class<?>> getClasses(Bootstrap.Mode mode) {
		if (mode == Bootstrap.Mode.INSTALL) {
			return install.getClasses();
		} else {
			return hotswap.getClasses();
		}
	}

	public Set<Class<?>> findInjections(Class<? extends Annotation> annotation) {
		if (hotswap == null) {
			return ClassUtils.findInjections(install.getClasses().values(), annotation);
		} else {
			return ClassUtils.findInjections(hotswap.getClasses().values(), annotation);
		}
	}

	public Object findImplement(Bootstrap.Mode mode, String interfaceName) {
		if (mode == Bootstrap.Mode.INSTALL) {
			return install.getExports().get(interfaceName);
		} else {
			return hotswap.getExports().get(interfaceName);
		}
	}

	public void registerImplement(Bootstrap.Mode mode, String interfaceName, Object instance) {
		if (mode == Bootstrap.Mode.INSTALL) {
			install.getExports().put(interfaceName, instance);
		} else {
			hotswap.getExports().put(interfaceName, instance);
		}
	}

	public void inject(ApplicationContext context, Bootstrap.Mode mode) throws Throwable {
		Map<String, Object> exports;
		if (mode == Bootstrap.Mode.INSTALL) {
			exports = install.getExports();
		} else {
			exports = hotswap.getExports();
		}
		for (Object export : exports.values()) {
			if (ClassUtils.hasAnnotation(export.getClass(), Autolink.class)) {
				Injectors injectors = Injectors.of(export.getClass(), Autolink.class);
				injectors.inject(context, this, export);
				System.out.println("inject instance: " + export.getClass());
			}
		}
	}

	/**
	 * hotswap
	 *
	 * @return {@code true}执行hotswap，{@code false}跳过hotswap
	 * @throws IOException hotswap异常
	 */
	public boolean hotswap() throws IOException {
		if (!(this.install instanceof PackageNamespace)) {
			this.hotswap = this.install;
			return false;
		}
		PackageNamespace namespace = (PackageNamespace) this.install;
		String subpackage = namespace.getSubpackage();
		String root = namespace.getRoot();
		Set<String> classNames = ClassUtils.findClasses(subpackage, true);
		Map<String, byte[]> oldCache = namespace.getClassBytes();
		Map<String, byte[]> newCache = new HashMap<String, byte[]>(classNames.size());
		if (oldCache.size() == classNames.size()) {
			for (String className : classNames) {
				newCache.put(className, FileUtils.readClass(className));
			}
		}
		if (EqualsUtils.isEquals(oldCache, newCache)) {
			this.hotswap = this.install;
			System.out.println("[hotswap: N]: " + name);
			return false;
		}
		System.out.println("[hotswap: Y]: " + name);
		ClassLoader classLoader = namespace.getClassLoader();
		ClassLoader parent = ((HotswapClassLoader) classLoader).getParentClassLoader();
		HotswapClassLoader newLoader = new HotswapClassLoader(name, parent, root);
		this.hotswap = new PackageNamespace(newLoader, root, subpackage);
		for (Map.Entry<String, byte[]> entry : newCache.entrySet()) {
			String className = entry.getKey();
			byte[] bytes = entry.getValue();
			Class<?> clazz = newLoader.loadClass(entry.getKey(), bytes);
			this.hotswap.getClassBytes().put(className, bytes);
			this.hotswap.getClasses().put(className, clazz);
		}
		return true;
	}

	public void runListeners() throws Throwable {
		listeners.forEach(ThrowableUtils.wrapper(Listener::apply));
	}

	public void finishHotswap() {
		install = hotswap;
		hotswap = null;
	}

	/**
	 * 根据根节点生成一个{@link Assembly}
	 *
	 * @param root 根节点
	 * @return 根Assembly
	 * @throws ClassNotFoundException 类查找失败
	 */
	public static Assembly of(Object root) throws ClassNotFoundException {
		Namespace namespace = new RootNamespace(root);
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
	public static Assembly of(ClassLoader parent, String root, String subpackage) throws IOException {
		HotswapClassLoader classLoader = new HotswapClassLoader(subpackage, parent, root);
		Namespace namespace = new PackageNamespace(classLoader, root, subpackage);
		Set<String> classNames = ClassUtils.findClasses(subpackage, true);
		for (String className : classNames) {
			byte[] bytes = FileUtils.readClass(className);
			Class<?> clazz = classLoader.loadClass(className, bytes);
			namespace.getClassBytes().put(className, bytes);
			namespace.getClasses().put(className, clazz);
		}
		return new Assembly(subpackage, namespace);
	}

	/**
	 * 获取{@code Assembly}名称
	 *
	 * @return {@code Assembly}名称
	 */
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "assembly: name = '" + name + "'";
	}
}
