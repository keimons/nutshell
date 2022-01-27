package com.keimons.nutshell.core;

/**
 * 可安装的
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 9
 **/
public interface Installable {

	/**
	 * 安装
	 *
	 * @param instance 模块
	 * @throws Throwable 安装异常
	 */
	void install(Object instance) throws Throwable;
}
