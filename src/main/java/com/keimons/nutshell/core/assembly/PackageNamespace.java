package com.keimons.nutshell.core.assembly;

import com.keimons.nutshell.core.internal.HotswapClassLoader;

import java.io.IOException;

public class PackageNamespace extends AbstractNamespace<HotswapClassLoader> {

	final String root;

	final String subpackage;

	final ClassLoader parent;

	public PackageNamespace(HotswapClassLoader classLoader, String root, String subpackage) throws IOException {
		super(classLoader);
		this.root = root;
		this.parent = classLoader.getParentClassLoader();
		this.subpackage = subpackage;
	}

	public PackageNamespace(PackageNamespace namespace) {
		super(namespace.classLoader, namespace.classBytes, namespace.classes, namespace.exports);
		this.root = namespace.root;
		this.parent = namespace.parent;
		this.subpackage = namespace.subpackage;
	}

	public ClassLoader getParentClassLoader() {
		return parent;
	}

	public PackageNamespace getBackup() {
		return this;
	}

	public String getRoot() {
		return root;
	}

	public String getSubpackage() {
		return subpackage;
	}
}
