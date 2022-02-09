package com.keimons.nutshell.core.monitor;

import com.keimons.nutshell.core.NutshellApplication;

import java.io.File;

/**
 * ApplicationMonitor
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class ApplicationMonitor implements Runnable {

	Object last;

	private final NutshellApplication application;

	private final ApplicationObserver<Object> observer;

	private String dir;

	private String pkg;

	@SuppressWarnings("unchecked")
	public ApplicationMonitor(NutshellApplication application, ApplicationObserver<?> observer, Object root) {
		this.application = application;
		this.observer = (ApplicationObserver<Object>) observer;
		String path = root.getClass().getResource("/").getPath();
		String packagePath = root.getClass().getPackageName().replaceAll("\\.", File.separator);
		this.dir = path + packagePath + File.separator;
		this.pkg = root.getClass().getPackageName();
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
