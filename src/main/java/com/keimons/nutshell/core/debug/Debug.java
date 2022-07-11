package com.keimons.nutshell.core.debug;

/**
 * Debug
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
public class Debug {

	public static void logSafePoint(SafePoint type) {
		System.out.println(type.msg);
	}

	public static void logSafePoint(SafePoint type, String message) {
		System.out.println(type.msg + message);
	}

	public enum SafePoint {
		PARK("[SafePoint][  park] "),
		READY("[SafePoint][ ready]"),
		UNPARK("[SafePoint][unpark]");

		String msg;

		SafePoint(String msg) {
			this.msg = msg;
		}
	}
}
