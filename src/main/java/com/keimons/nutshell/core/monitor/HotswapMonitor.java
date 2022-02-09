package com.keimons.nutshell.core.monitor;

import com.keimons.nutshell.core.NutshellApplication;
import com.keimons.nutshell.core.internal.utils.PackageUtils;

import java.io.File;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * 热插拔监视器
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class HotswapMonitor implements Runnable {

	private final NutshellApplication application;

	private final HotswapObserver<Object> observer;

	private final String rootPackage;

	private final String rootPath;

	private volatile boolean running;

	private Thread thread;

	private Object last = "1";

	private String dir;

	private int interval;

	@SuppressWarnings("unchecked")
	public HotswapMonitor(NutshellApplication application, HotswapObserver<?> observer, Object root, int interval) {
		this.application = application;
		this.observer = (HotswapObserver<Object>) observer;
		this.rootPackage = root.getClass().getPackageName();
		this.rootPath = this.rootPackage.replaceAll("\\.", Matcher.quoteReplacement(File.separator));
		String path = root.getClass().getResource("/").getPath();
		this.dir = path + this.rootPath + File.separator;
		this.interval = interval;
	}

	public void start() {
		if (!running) {
			this.running = true;
			thread = new Thread(this, "Hotswap Monitor");
			thread.setDaemon(true);
			thread.start();
		}
	}

	public void stop() {
		if (this.running) {
			this.running = false;
			try {
				this.thread.interrupt();
				this.thread.join(interval);
			} catch (InterruptedException var5) {
				Thread.currentThread().interrupt();
			}
		}
	}

	@Override
	public void run() {
		while (running) {
			Object messageInfo = observer.getMessageInfo();
			if (!last.equals(messageInfo)) {
				File file = observer.getHotswapFile(messageInfo);
				if (file.isDirectory()) {
					Set<String> subpackages = PackageUtils.findSubpackages(rootPackage);
					try {
						application.hotswap(subpackages.toArray(new String[0]));
						System.gc();
					} catch (Throwable e) {
						e.printStackTrace();
					}
					last = messageInfo;
				} else {
					// TODO jar file
				}
			}
			try {
				Thread.sleep(interval);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
