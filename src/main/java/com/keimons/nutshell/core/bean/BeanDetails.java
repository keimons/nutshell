package com.keimons.nutshell.core.bean;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Bean生成器
 * <p>
 * 存储用于生成Bean的信息，生成Bean对象。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public interface BeanDetails<T> {

	/**
	 * 生成一个Bean对象
	 *
	 * @return Bean对象
	 */
	T newInstance();

	/**
	 * Bean对象的关键词关键词
	 * <p>
	 * 对于Bean对象的关键词分为三类：名字、接口和类名。
	 *
	 * @return Bean对象的关键词
	 */
	Keywords getKeywords();

	/**
	 * 关键词
	 */
	public static class Keywords {

		String[] names;

		Class<?>[] classes;

		public String[] getNames() {
			return names;
		}

		public Class<?>[] getClasses() {
			return classes;
		}

		private static String[] findNames(Class<?> clazz) {
			return null;
		}

		private static Class<?>[] findClasses(Class<?> clazz) {
			ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
			if (!Modifier.isAbstract(clazz.getModifiers()) && !clazz.isInterface()) {
				classes.add(clazz);
			}
			while (clazz != Object.class) {
				classes.addAll(Arrays.asList(clazz.getInterfaces()));
				clazz = clazz.getSuperclass();
			}
			return classes.toArray(new Class<?>[0]);
		}

		public static Keywords of(Class<?> clazz) {
			Keywords keywords = new Keywords();
			keywords.names = findNames(clazz);
			keywords.classes = findClasses(clazz);
			return keywords;
		}
	}
}
