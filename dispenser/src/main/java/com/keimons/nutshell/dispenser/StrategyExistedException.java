package com.keimons.nutshell.dispenser;

/**
 * StrategyExistedException
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class StrategyExistedException extends RuntimeException {

	public StrategyExistedException() {

	}

	public StrategyExistedException(String message) {
		super(message);
	}
}
