package com.keimons.nutshell.core.internal.utils;

import java.io.File;

/**
 * 运行时工具
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
public class RuntimeUtils {

	private static boolean init;

	private static boolean dev;

	private static String path;

	public static void init(Class<?> root) {
		if (!init) {
			init = true;
			File file = new File(root.getProtectionDomain().getCodeSource().getLocation().getPath());
			dev = file.isFile();
			path = root.getResource("/").getPath();
		}
	}

	public static boolean isRunInClass() {
		if (!init) {
			throw new IllegalStateException("RuntimeUtils not init.");
		}
		return dev;
	}

	public static String path() {
		if (!init) {
			throw new IllegalStateException("RuntimeUtils not init.");
		}
		return path;
	}
}
