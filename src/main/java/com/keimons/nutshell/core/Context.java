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
public interface Context {

	/**
	 * 添加{@link Assembly}
	 *
	 * @param assembly 程序集
	 */
	void add(Assembly assembly);

	/**
	 * 获取{@link Assembly}
	 *
	 * @param name {@link Assembly}名称
	 * @return 名称对应的{@link Assembly}
	 */
	Assembly get(String name);

	Map<String, Assembly> getAssemblies();

	Map<String, Assembly> getInstances();

	/**
	 * 初始化
	 *
	 * @param assembly 程序集
	 * @throws Throwable 初始化异常
	 */
	void init(Assembly assembly) throws Throwable;

	/**
	 * 安装
	 *
	 * @param assembly 安装程序集
	 * @throws Throwable 安装异常
	 */
	void install(Assembly assembly) throws Throwable;

	/**
	 * 链接
	 *
	 * @throws Throwable 连接异常
	 */
	void link(Assembly assembly) throws Throwable;

	/**
	 * 查找接口所在{@link Assembly}
	 * <p>
	 * 每个接口只会存在于一个{@link Assembly}中。
	 *
	 * @param className 接口名称
	 */
	Assembly findInstance(String className);
}
