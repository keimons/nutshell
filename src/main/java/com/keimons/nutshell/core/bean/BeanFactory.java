package com.keimons.nutshell.core.bean;

/**
 * BeanFactory
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
public interface BeanFactory {

	Object getBean(String name);

	Object getBean(Class<?> clazz);
}
