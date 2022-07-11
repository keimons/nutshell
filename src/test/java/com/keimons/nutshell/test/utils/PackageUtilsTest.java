package com.keimons.nutshell.test.utils;

import com.keimons.nutshell.core.internal.utils.PackageUtils;
import org.junit.jupiter.api.Test;

import java.util.Set;

/**
 * {@link PackageUtils}测试
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 **/
public class PackageUtilsTest {

	@Test
	public void test() {
		Set<String> packages = PackageUtils.findSubpackages("com.keimons.nutshell.test");
		for (String pkg : packages) {
			System.out.println(pkg);
		}
	}
}
