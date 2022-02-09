package com.keimons.nutshell.core.monitor;

import com.keimons.nutshell.core.internal.utils.FileUtils;

import java.io.File;

/**
 * 监视器
 * <p>
 * nutshell能够发现文件的变动，却无法确保变化的完整性和文件的完整性。
 * 应由程序提供检测时机和检测位置。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public abstract class AbstractObserver {

	String path;

	/**
	 * 观察者
	 *
	 * @param path
	 */
	public AbstractObserver(String path) {
		this.path = path;
	}

	public void checkAndNotify() {
		File file = new File(path);
		try {
			String info = FileUtils.readLastNotEmptyLine(file);
			String[] split = info.split(",");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 检测是否发生变化
	 * <p>
	 * 注意：不仅仅是发现新版本，同时也应该对文件完整性进行校验。
	 *
	 * @return 是否发生变化
	 */
	public abstract String checkAndGetVersion();

	/**
	 * 获取文件
	 * <p>
	 * 在dev模式下和在部署模式下返回不同文件。
	 * <ul>
	 *     <li>直接运行class，则返回class的根目录。</li>
	 *     <li>运行jar包，则返回该版本对应的jar包。</li>
	 * </ul>
	 *
	 * @return 文件
	 */
	public abstract String getByDevelopment(int version);

	public abstract File getByDeploy(int version);
}
