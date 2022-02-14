package com.keimons.nutshell.core.assembly;

import java.io.IOException;

/**
 * RootAssembly
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class RootAssembly extends AbstractAssembly<RootNamespace> {

	public RootAssembly(String name, RootNamespace namespace) {
		super(name, namespace);
	}

	public boolean fork() throws IOException {
		return false;
	}

	public void join(boolean success) {
		// do nothing
	}
}
