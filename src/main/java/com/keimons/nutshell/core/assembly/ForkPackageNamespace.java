package com.keimons.nutshell.core.assembly;

import com.keimons.nutshell.core.internal.HotswapClassLoader;

import java.io.IOException;

/**
 * ForkNamespace
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
public class ForkPackageNamespace extends PackageNamespace {

	final PackageNamespace backup;

	public ForkPackageNamespace(HotswapClassLoader classLoader, PackageNamespace backup) throws IOException {
		super(classLoader, backup.root, backup.subpackage);
		this.backup = backup;
	}

	public PackageNamespace getBackup() {
		return backup;
	}
}
