package com.keimons.nutshell.core.internal.utils;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * UnsafeUtils
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class UnsafeUtils {

	private static Unsafe unsafe = null;

	static {
		try {
			Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
			unsafeField.setAccessible(true);
			unsafe = (sun.misc.Unsafe) unsafeField.get(null);
		} catch (Exception e) {
			e.printStackTrace(); //ignore
		}
	}

	public static Unsafe getUnsafe() {
		return unsafe;
	}
}
