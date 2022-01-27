package com.keimons.nutshell.core.internal;

import jdk.internal.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * {@link MethodHandle}工具类，用于提供可信任的{@link MethodHandles.Lookup}。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 9
 **/
public class LookupUtils {

	private static final MethodHandles.Lookup lookup;

	static {
		MethodHandles.Lookup lookup0 = null;
		Unsafe unsafe = Unsafe.getUnsafe();
		try {
			// 尝试查找受信任的包级私有的 MethodHandles.Lookup#IMPL_LOOKUP
			long offset = unsafe.staticFieldOffset(MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP"));
			lookup0 = (MethodHandles.Lookup) unsafe.getObject(MethodHandles.Lookup.class, offset);
		} catch (Exception e) {
			try {
				// 查找失败 尝试自己生成一个具有所有权限的lookup。
				lookup0 = (MethodHandles.Lookup) unsafe.allocateInstance(MethodHandles.Lookup.class);
				long offset1 = unsafe.staticFieldOffset(MethodHandles.Lookup.class.getDeclaredField("lookupClass"));
				long offset2 = unsafe.staticFieldOffset(MethodHandles.Lookup.class.getDeclaredField("allowedModes"));
				unsafe.putObject(lookup0, offset1, Object.class);
				unsafe.putInt(lookup0, offset2, -1);
			} catch (Exception x) {
				x.printStackTrace();
			}
		}
		lookup = lookup0;
	}

	/**
	 * 可信任的{@link MethodHandles.Lookup}。
	 * <p>
	 * 这是一个可信任的{@code Lookup}，它具有所有的访问权限，慎用。
	 *
	 * @return 可信任的{@link MethodHandles.Lookup}
	 */
	public static MethodHandles.Lookup lookup() {
		return lookup;
	}
}
