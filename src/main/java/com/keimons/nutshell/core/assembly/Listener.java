package com.keimons.nutshell.core.assembly;

import com.keimons.nutshell.core.Autolink;

/**
 * 监听器
 * <p>
 * 每一个{@link Autolink}都会注册两个监听器，用于监听{@link Assembly}的安装、更新和卸载。监听器有两种类型：
 * <ul>
 *     <li>{@link ListenerType#INSTALL}安装</li>
 *     <li>{@link ListenerType#UPGRADE}升级</li>
 * </ul>
 * 当发生{@link Assembly}安装、更新和卸载时，相关联的{@link Assembly}也需要进行相应的更新。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 9
 **/
@FunctionalInterface
public interface Listener {

	/**
	 * 执行
	 *
	 * @throws Throwable 链接异常
	 */
	void apply() throws Throwable;

	/**
	 * 监听类型
	 */
	enum ListenerType {
		INSTALL, UPGRADE
	}
}
