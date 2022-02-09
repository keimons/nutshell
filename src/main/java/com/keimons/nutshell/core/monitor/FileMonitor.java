package com.keimons.nutshell.core.monitor;

import com.keimons.nutshell.core.NutshellApplication;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * FileMonitor
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class FileMonitor {

	NutshellApplication application;

	String root;

	String dir;

	String pkg;

	public FileMonitor(NutshellApplication application, Object root) {
		this.application = application;
		this.root = root.getClass().getResource("/").getPath();
		String packagePath = root.getClass().getPackageName().replaceAll("\\.", File.separator);
		this.dir = this.root + packagePath + File.separator;
		this.pkg = root.getClass().getPackageName();
	}

	/**
	 * 文件监控之测试类
	 *
	 * @throws Exception 监控异常
	 */
	private void listen() throws Exception {
		// 轮询间隔 1 秒
		long interval = TimeUnit.SECONDS.toMillis(1);
		// 创建一个文件观察器用于处理文件的格式
		FileAlterationObserver observer = new FileAlterationObserver(dir);
		// 设置文件变化监听器
		observer.addListener(new FileListener(application, dir, pkg));
		// 创建文件变化监听器
		FileAlterationMonitor monitor = new FileAlterationMonitor(interval, observer);
		// 开始监控
		monitor.start();
	}

	/**
	 * 开始监控
	 */
	public void monitor() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					listen();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
}
