package com.keimons.nutshell.core.support;

/**
 * 指定类型的一个生成器
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
public class ClassedBeanDetails<T> extends AbstractBeanDetails<T> {

	private Class<?> clazz;

	public ClassedBeanDetails(Class<?> clazz) {
		super(clazz);
	}

	@Override
	@SuppressWarnings("unchecked")
	public T newInstance() {
		try {
			return (T) clazz.getConstructor().newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
