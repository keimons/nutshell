package com.keimons.nutshell.core.boot;

import com.keimons.nutshell.core.Context;
import com.keimons.nutshell.core.assembly.Assembly;

/**
 * 引导安装
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 9
 **/
public interface Bootstrap {

	/**
	 * 安装模块
	 *
	 * @param context  上下文环境
	 * @param mode     安装模式
	 * @param assembly 本次安装的模块
	 * @throws Throwable 引导安装异常，模块安装中，可能发生各种异常，所以，不能确定模块安装是否成功。
	 */
	void invoke(Context context, Mode mode, Assembly assembly) throws Throwable;

	enum Mode {
		INSTALL, UPGRADE
	}
}
