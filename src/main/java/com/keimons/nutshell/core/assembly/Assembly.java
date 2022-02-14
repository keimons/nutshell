package com.keimons.nutshell.core.assembly;

import com.keimons.nutshell.core.ApplicationContext;
import com.keimons.nutshell.core.internal.HotswapClassLoader;
import com.keimons.nutshell.core.internal.utils.ClassUtils;
import com.keimons.nutshell.core.internal.utils.FileUtils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;

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
public interface Assembly {

	/**
	 * 根
	 */
	String ROOT = "ROOT";

	/**
	 * 获取{@code Assembly}名称
	 *
	 * @return {@code Assembly}名称
	 */
	String getName();

	/**
	 * 获取{@code Assembly}所有的类
	 *
	 * @return {@code Assembly}所有的类
	 */
	Map<String, Class<?>> getClasses();

	/**
	 * 获取{@code Assembly}所有包含该注解的类
	 *
	 * @param annotation 注解
	 * @return 包含该注解的类
	 */
	Set<Class<?>> findInjections(Class<? extends Annotation> annotation);

	/**
	 * 查找接口实现对象
	 *
	 * @param interfaceName 接口名
	 * @return 接口实现对象
	 */
	Object findImplement(String interfaceName);

	/**
	 * 注册接口实现对象
	 *
	 * @param interfaceName 接口名
	 * @param instance      接口实现对象
	 */
	void registerImplement(String interfaceName, Object instance);

	/**
	 * 注入
	 *
	 * @param context 上下文环境
	 * @throws Throwable 注入异常
	 */
	void inject(ApplicationContext context) throws Throwable;

	/**
	 * 切出分支
	 *
	 * @return {@code true}执行hotswap，{@code false}跳过hotswap
	 * @throws IOException hotswap异常
	 */
	boolean fork() throws IOException;

	/**
	 * 合并分支
	 *
	 * @param success 更新是否成功
	 */
	void join(boolean success);

	/**
	 * 增加监听器
	 *
	 * @param listener 监听器
	 */
	void addListener(Listener listener);

	/**
	 * 执行所有监听器
	 *
	 * @throws Throwable 异常
	 */
	void runListeners() throws Throwable;

	/**
	 * 根据根节点生成一个{@link Assembly}
	 *
	 * @param root 根节点
	 * @return 根Assembly
	 * @throws ClassNotFoundException 类查找失败
	 */
	static Assembly of(Object root) throws ClassNotFoundException {
		RootNamespace namespace = new RootNamespace(root);
		return new RootAssembly(ROOT, namespace);
	}

	/**
	 * 生成一个{@link Assembly}
	 *
	 * @param parent     父类装载器
	 * @param root       类装载器的根目录
	 * @param subpackage {@link Assembly}目录
	 * @return {@link Assembly}
	 */
	static Assembly of(ClassLoader parent, String root, String subpackage) throws IOException {
		HotswapClassLoader classLoader = new HotswapClassLoader(subpackage, parent, root);
		PackageNamespace namespace = new PackageNamespace(classLoader, root, subpackage);
		Set<String> classNames = ClassUtils.findClasses(subpackage, true);
		for (String className : classNames) {
			byte[] bytes = FileUtils.readClass(className);
			Class<?> clazz = classLoader.loadClass(className, bytes);
			namespace.getClassBytes().put(className, bytes);
			namespace.getClasses().put(className, clazz);
		}
		return new PackageAssembly(subpackage, namespace);
	}
}
