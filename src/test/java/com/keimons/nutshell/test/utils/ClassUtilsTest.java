package com.keimons.nutshell.test.utils;

import com.keimons.nutshell.core.internal.utils.NClassUtils;
import org.junit.jupiter.api.Test;

import java.util.Set;

public class ClassUtilsTest {

	@Test
	public void test() {
		Set<Class<?>> classes = NClassUtils.findClasses(null, "com.keimons.nutshell.test.link", false);
		System.out.println(classes);
	}
}
