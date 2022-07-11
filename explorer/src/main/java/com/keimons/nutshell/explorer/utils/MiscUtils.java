package com.keimons.nutshell.explorer.utils;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * 各种工具类
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
public class MiscUtils {

	public static VarHandle findVarHandle(Class<?> clazz, String name, Class<?> type) {
		try {
			MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(clazz, MethodHandles.lookup());
			return lookup.findVarHandle(clazz, name, type);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
