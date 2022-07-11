package com.keimons.nutshell.core.internal.utils;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

/**
 * MethodUtils
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
public class MethodUtils {

	public static MethodHandle findMethod(Class<?> clazz, String name) {
		Method action = null;
		for (Method method : clazz.getDeclaredMethods()) {
			if (method.getName().equals(name)) {
				if (action == null) {
					action = method;
				} else {
					throw new RuntimeException();
				}
			}
		}
		if (action == null) {
			throw new RuntimeException();
		}
		try {
			return LookupUtils.lookup().unreflect(action);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
