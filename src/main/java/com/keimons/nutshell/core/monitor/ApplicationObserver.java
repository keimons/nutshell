package com.keimons.nutshell.core.monitor;

import java.io.File;

/**
 * 监视器
 * <p>
 * nutshell能够发现文件的变动，却无法确保变化的完整性和文件的完整性。
 * 应由程序提供检测时机和检测位置。
 * <p>
 * 每间隔x秒调用一次{@link ApplicationObserver}，如果本次返回信息和上次返回信息一致，则不进行更新。
 * 如果返回信息有变化，则将最新信息传入{@link #getHotswapFile(Object)}并返回热插拔文件。
 *
 * @param <T> 观察类型
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public interface ApplicationObserver<T> {

	T getMessageInfo();

	File getHotswapFile(T message);
}
