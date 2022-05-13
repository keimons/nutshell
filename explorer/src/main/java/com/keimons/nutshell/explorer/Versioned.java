package com.keimons.nutshell.explorer;

/**
 * 版本化
 * <p>
 * 对象带有版本，不同的版本将会视作不同的对象。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
public interface Versioned {

	void serVersion(int version);
}
