package com.keimons.nutshell.explorer;

/**
 * 调试信息
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
public class Debug {

	public static boolean DEBUG = false;

	/**
	 * 信息
	 *
	 * @param msg 信息内容
	 */
	public static void info(String msg) {
		if (DEBUG) {
			System.out.println(msg);
		}
	}

	/**
	 * 警告
	 *
	 * @param msg 警告内容
	 */
	public static void warn(String msg) {
		if (DEBUG) {
			System.out.println(msg);
		}
	}
}
