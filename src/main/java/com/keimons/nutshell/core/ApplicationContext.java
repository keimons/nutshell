package com.keimons.nutshell.core;

import com.keimons.nutshell.core.assembly.Assembly;

import java.util.Map;

/**
 * 上下文环境
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 9
 **/
public interface ApplicationContext {

	/**
	 * 添加{@link Assembly}
	 *
	 * @param assembly 程序集
	 */
	void add(Assembly assembly);

	/**
	 * 获取{@link Assembly}
	 *
	 * @param packageName {@link Assembly}名称
	 * @return 名称对应的{@link Assembly}
	 */
	Assembly get(String packageName);

	Map<String, Assembly> getAssemblies();

	Map<String, Assembly> getInstances();

	/**
	 * 查找接口所在{@link Assembly}
	 * <p>
	 * 每个接口只会存在于一个{@link Assembly}中。
	 *
	 * @param className 接口名称
	 */
	Assembly findInstance(String className);
}
