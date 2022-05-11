package com.keimons.nutshell.explorer.utils;


import jdk.internal.vm.annotation.ForceInline;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

public class CASUtils {

	public static final Unsafe UNSAFE;
	public static final int ARRAY_OBJECT_INDEX_SCALE;
	public static final int ARRAY_OBJECT_BASE_OFFSET;

	static {
		try {
			PrivilegedExceptionAction<Unsafe> action = new PrivilegedExceptionAction<Unsafe>() {
				public Unsafe run() throws Exception {
					Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
					theUnsafe.setAccessible(true);
					return (Unsafe) theUnsafe.get((Object) null);
				}
			};
			UNSAFE = (Unsafe) AccessController.doPrivileged(action);
		} catch (Exception var1) {
			throw new RuntimeException("Unable to load unsafe", var1);
		}

		ARRAY_OBJECT_INDEX_SCALE = UNSAFE.arrayIndexScale(Object[].class);
		ARRAY_OBJECT_BASE_OFFSET = UNSAFE.arrayBaseOffset(Object[].class);
	}

	/**
	 * 查找字段偏移位置
	 *
	 * @param clazz 目标类
	 * @param name  目标字段
	 * @return 偏移位置
	 */
	public static long objectFieldOffset(Class<?> clazz, String name) {
		try {
			return UNSAFE.objectFieldOffset(clazz.getDeclaredField(name));
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
	}

	@ForceInline
	public static boolean cas(Object o, long offset, int expected, int newValue) {
		return UNSAFE.compareAndSwapInt(o, offset, expected, newValue);
	}

	@ForceInline
	public static boolean cas(Object o, long offset, long expected, long newValue) {
		return UNSAFE.compareAndSwapLong(o, offset, expected, newValue);
	}

	@ForceInline
	public static long casSet(Object o, long offset, int newValue) {
		int v;
		do {
			v = UNSAFE.getIntVolatile(o, offset);
		} while (!UNSAFE.compareAndSwapInt(o, offset, v, newValue));
		return v;
	}

	@ForceInline
	public static long casSet(Object o, long offset, long newValue) {
		long v;
		do {
			v = UNSAFE.getLongVolatile(o, offset);
		} while (!UNSAFE.compareAndSwapLong(o, offset, v, newValue));
		return v;
	}

	@ForceInline
	public static Object casSet(Object o, long offset, Object newValue) {
		Object v;
		do {
			v = UNSAFE.getObjectVolatile(o, offset);
		} while (!UNSAFE.compareAndSwapObject(o, offset, v, newValue));
		return v;
	}
}
