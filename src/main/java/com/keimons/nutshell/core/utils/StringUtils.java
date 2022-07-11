package com.keimons.nutshell.core.utils;

/**
 * 字符串工具类
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 **/
public class StringUtils {

	public static boolean isEmpty(String value) {
		return value == null || value.trim().equals("");
	}
}
