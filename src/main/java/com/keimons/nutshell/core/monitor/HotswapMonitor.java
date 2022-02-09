package com.keimons.nutshell.core.monitor;

import com.keimons.nutshell.core.NutshellApplication;

import java.io.File;

/**
 * 热插拔监视器
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class HotswapMonitor implements Runnable {

	Object last;

	private final NutshellApplication application;

	private final HotswapObserver<Object> observer;

	private String dir;

	private String pkg;

	@SuppressWarnings("unchecked")
	public HotswapMonitor(NutshellApplication application, HotswapObserver<?> observer, Object root) {
		this.application = application;
		this.observer = (HotswapObserver<Object>) observer;
		this.pkg = root.getClass().getPackageName();
		String path = root.getClass().getResource("/").getPath();
		String packagePath = this.pkg.replaceAll("\\.", File.separator);
		this.dir = path + packagePath + File.separator;
	}

	public void monitor() {
		Thread thread = new Thread(this);
		thread.start();
	}

	@Override
	public void run() {
		Object messageInfo = observer.getMessageInfo();
		if (!last.equals(messageInfo)) {
			File file = observer.getHotswapFile(messageInfo);
			if (file.isDirectory()) {
				String absolutePath = file.getAbsolutePath();
				String substring = absolutePath.substring(dir.length(), absolutePath.indexOf(File.separator, dir.length()));
				String subpackage = pkg + "." + substring;
				try {
					application.hotswap(subpackage);
					System.gc();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			} else {
				// TODO jar file
			}
		}
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
