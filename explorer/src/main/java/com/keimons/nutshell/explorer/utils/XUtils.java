package com.keimons.nutshell.explorer.utils;


import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class XUtils {

	public static VarHandle findVarHandle(Class<?> clazz, String name, Class<?> type) {
		try {
			MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(clazz, MethodHandles.lookup());
			return lookup.findVarHandle(clazz, name, type);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
