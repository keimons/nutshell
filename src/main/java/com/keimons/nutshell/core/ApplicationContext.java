package com.keimons.nutshell.core;

import com.keimons.nutshell.core.assembly.Assembly;
import com.keimons.nutshell.core.internal.HotswapClassLoader;
import com.keimons.nutshell.core.internal.namespace.Namespace;

import java.util.Map;

/**
 * 上下文环境
 * <p>
 * 存储应用程序或服务的运行时信息，包括：
 * <ul>
 *     <li>应用程序或服务的所有{@link Assembly}</li>
 *     <li>注入点的接口对应实现</li>
 * </ul>
 * 上下文环境中，所有的{@code key}都应该是用字符串。参考{@link HotswapClassLoader}，
 * 同一个{@code interface}由不同的类装载器装载，造成{@code com.A != com.A}的假象。
 * 我们只能使用{@link HotswapClassLoader}装载器装载范围以外的对象作为{@code key}。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
public interface ApplicationContext {

	/**
	 * 添加{@link Assembly}
	 *
	 * @param assembly 程序集
	 */
	void add(Assembly assembly);

	/**
	 * 根据名称查找{@link Assembly}
	 *
	 * @param assemblyName {@link Assembly}名称
	 * @return 名称对应的Assembly
	 */
	Assembly findAssembly(String assemblyName);

	/**
	 * 获取所有名称对应的{@link Assembly}
	 *
	 * @return 所有Assembly
	 */
	Map<String, Assembly> getAssemblies();

	/**
	 * 根据接口查找{@link Assembly}
	 * <p>
	 * 每个接口只会存在于一个{@link Assembly}中。
	 *
	 * @param interfaceName 接口名称
	 */
	Assembly findImplement(String interfaceName);

	/**
	 * 获取所有接口对应的{@link Assembly}
	 * <p>
	 * 上下文环境中不会直接存储接口实现，而是存储接口实现所在的Assembly，真正的实现存放于{@link Namespace}。
	 *
	 * @return 接口对应实例映射
	 */
	Map<String, Assembly> getImplements();
}
