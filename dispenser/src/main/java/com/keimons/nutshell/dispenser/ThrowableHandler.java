package com.keimons.nutshell.dispenser;

/**
 * ThrowableHandler
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public interface ThrowableHandler {

	void handler(Runnable runnable, Throwable cause);
}
