package com.keimons.nutshell.core.bootstrap;

import com.keimons.nutshell.core.ApplicationContext;
import com.keimons.nutshell.core.assembly.Assembly;

import java.util.List;

/**
 * 引导安装/更新
 * <p>
 * 引导{@link Assembly}的安装和更新，安装过程分为多个步骤，每一个步骤可能失败。
 * <ul>
 *     <li>安装：终止启动。</li>
 *     <li>更新：放弃更新。</li>
 * </ul>
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
public interface Bootstrap {

	/**
	 * 安装模块
	 *
	 * @param context  上下文环境
	 * @param assembly 本次安装的模块
	 * @throws Throwable 引导安装异常，模块安装中，可能发生各种异常，所以，不能确定模块安装是否成功。
	 */
	void install(ApplicationContext context, Assembly assembly) throws Throwable;

	/**
	 * 更新模块
	 *
	 * @param context  上下文环境
	 * @param assembly 本次安装的模块
	 * @param outbound 流向下一个引导步
	 * @throws Throwable 引导安装异常，模块安装中，可能发生各种异常，所以，不能确定模块安装是否成功。
	 */
	void hotswap(ApplicationContext context, Assembly assembly, List<Assembly> outbound) throws Throwable;
}
