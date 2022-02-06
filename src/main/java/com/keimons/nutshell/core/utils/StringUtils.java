package com.keimons.nutshell.core.utils;

public class StringUtils {

	public static boolean isEmpty(String value) {
		return value == null || value.trim().equals("");
	}
}
