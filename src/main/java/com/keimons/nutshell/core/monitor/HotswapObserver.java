package com.keimons.nutshell.core.monitor;

import java.io.File;

/**
 * 监视器
 * <p>
 * nutshell能够发现文件的变动，却无法确保变化的完整性和文件的完整性。应由程序或服务提供检测时机和检测位置。
 * <p>
 * 每间隔x秒调用一次{@link HotswapObserver}，如果本次返回信息和上次返回信息一致（通过{@link Object#equals(Object)}判断），
 * 则不进行更新。如果返回信息有变化，则将最新信息传入{@link #getHotswapFile(Object)}并返回热插拔文件。
 * <p>
 * hotswap的触发，应该位于确保所有hotswap文件完整后，此部分需要应用程序或服务提供。
 *
 * @param <T> 观察类型
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @see IdeaHotswapObserver IntelliJ IDEA运行时目录监控
 * @since 11
 */
public interface HotswapObserver<T> {

	/**
	 * 获取最新版本
	 * <p>
	 * 系统启动时，会调用此方法初始化版本信息。版本比较使用{@link Object#equals(Object)}。
	 * <p>
	 * 注意：如果在读取版本信息时发生异常，应直接抛出运行时异常。初始化版本信息时异常，则直接启动失败。
	 * 运行时检测异常，忽略本次检测。等待下一个检测时间点。
	 *
	 * @return 最新版本
	 */
	T getNextVersion();

	/**
	 * 获取hotswap文件
	 * <p>
	 * 当且仅当版本发生变化时，调用此方法。hotswap文件可以是jar文件或者包含{@link Class}的文件夹。
	 * 如果是文件夹，则{@link File}应该指向class根目录，例如：com.keimons.X，应该指向com所在目录。
	 *
	 * @param version 版本
	 * @return 版本对应的hotswap文件
	 * @see IdeaHotswapObserver IntelliJ IDEA运行时目录监控
	 */
	File getHotswapFile(T version);

	// TODO add hotswap finish notice.
}
