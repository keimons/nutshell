package com.keimons.nutshell.core.support;

/**
 * 单实例Bean生成器
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class InstancedBeanDetails<T> extends AbstractBeanDetails<T> {

	private final T instance;

	public InstancedBeanDetails(T instance) {
		super(instance.getClass());
		this.instance = instance;
	}

	@Override
	public T newInstance() {
		return instance;
	}
}
