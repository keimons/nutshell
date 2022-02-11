package com.keimons.nutshell.core.monitor;

import java.io.File;

/**
 * IdeaHotswapObserver
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public abstract class IdeaHotswapObserver<T> implements HotswapObserver<T> {

	@Override
	public File getHotswapFile(T version) {
		return new File(this.getClass().getResource("/").getPath());
	}
}
