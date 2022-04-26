package com.keimons.nutshell.core.support;

import com.keimons.nutshell.core.bean.BeanDetails;

/**
 * AbstractBeanGenerator
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public abstract class AbstractBeanDetails<T> implements BeanDetails<T> {

	protected Keywords keywords;

	public AbstractBeanDetails(Class<?> clazz) {
		this.keywords = Keywords.of(clazz);
	}

	@Override
	public Keywords getKeywords() {
		return keywords;
	}
}
