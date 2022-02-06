package com.keimons.nutshell.test.utils;

import com.keimons.nutshell.core.internal.utils.PackageUtils;
import org.junit.jupiter.api.Test;

import java.util.Set;

public class PackageUtilTest {

	@Test
	public void test() {
		Set<String> packages = PackageUtils.findSubpackages("com.keimons.nutshell.test");
		for (String pkg : packages) {
			System.out.println(pkg);
		}
	}
}
