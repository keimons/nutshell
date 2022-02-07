package com.keimons.nutshell.test.utils;

import com.keimons.nutshell.core.internal.utils.ClassUtils;
import org.junit.jupiter.api.Test;

import java.util.Set;

/**
 * {@link ClassUtils}测试
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
public class ClassUtilsTest {

	@Test
	public void test() {
		Set<Class<?>> classes = ClassUtils.findClasses(null, "com.keimons.nutshell.test.link", false);
		System.out.println(classes);
	}
}
