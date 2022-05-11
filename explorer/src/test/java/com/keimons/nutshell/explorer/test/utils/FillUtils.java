package com.keimons.nutshell.explorer.test.utils;

public class FillUtils {

	public static <E> void fill(E[] array, Factory<E> factory) {
		for (int i = 0; i < array.length; i++) {
			array[i] = factory.newInstance();
		}
	}

	public interface Factory<E> {

		E newInstance();
	}
}
