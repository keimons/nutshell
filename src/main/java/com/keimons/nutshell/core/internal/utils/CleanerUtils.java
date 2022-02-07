package com.keimons.nutshell.core.internal.utils;

import java.lang.ref.Cleaner;

/**
 * 垃圾回收工具
 * <p>
 * 监控对象回收情况
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class CleanerUtils {

	private static final Cleaner cleaner = Cleaner.create();

	/**
	 * 注册一个{@link Class}的垃圾回收监控
	 *
	 * @param clazz 要监控的类对象
	 */
	public static void register(Class<?> clazz) {
		final String name = clazz.toString();
		cleaner.register(clazz, () -> System.out.println("class unload: " + name));
	}

	public static void register(Object object) {
		final String name = object.toString();
		cleaner.register(object, () -> System.out.println("class unload: " + name));
	}
}
