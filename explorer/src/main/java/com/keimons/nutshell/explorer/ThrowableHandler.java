package com.keimons.nutshell.explorer;

/**
 * ThrowableHandler
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
public interface ThrowableHandler {

	void handler(Runnable runnable, Throwable cause);
}
