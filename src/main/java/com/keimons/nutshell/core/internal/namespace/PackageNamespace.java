package com.keimons.nutshell.core.internal.namespace;

import com.keimons.nutshell.core.internal.HotswapClassLoader;

import java.io.IOException;

public class PackageNamespace extends AbstractNamespace {

	private final String root;

	private final String subpackage;

	public PackageNamespace(HotswapClassLoader classLoader, String root, String subpackage) throws IOException {
		super(classLoader);
		this.root = root;
		this.subpackage = subpackage;
	}

	public String getRoot() {
		return root;
	}

	public String getSubpackage() {
		return subpackage;
	}
}
