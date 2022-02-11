package com.keimons.nutshell.core.monitor;

import com.keimons.nutshell.core.NutshellApplication;
import com.keimons.nutshell.core.assembly.Assembly;
import com.keimons.nutshell.core.internal.utils.PackageUtils;

import java.io.File;
import java.util.Objects;
import java.util.Set;

/**
 * 热插拔监视器
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class HotswapMonitor implements Runnable {

	/**
	 * 应用程序
	 */
	private final NutshellApplication application;

	/**
	 * hotswap观察者
	 * <p>
	 * 通过调用观察者的{@link HotswapObserver#getNextVersion()}和当前的{@link #lastVersion}比较，
	 * 如果发生了变动，则调用{@link HotswapObserver#getHotswapFile(Object)}}获取变动文件信息。
	 */
	private final HotswapObserver<Object> observer;

	/**
	 * 监控根目录
	 * <p>
	 * 按照package划分的{@link Assembly}，当扫描到hotswap时，扫描rootPackage中的变动
	 */
	private final String rootPackage;

	/**
	 * 运行间隔（毫秒）
	 * <p>
	 * 每间隔interval毫秒，调用一次{@link #observer}，测试是否发生变化。
	 */
	private final int interval;

	/**
	 * hotswap监控是否运行
	 * <p>
	 * {@code true}监控中，{@code false}未监控。当停止监控时，{@link #thread}线程停止。
	 * <p>
	 * 注意：不会启动一个重复的监控，监控只有一个。
	 */
	private volatile boolean running;

	/**
	 * 监控的线程
	 */
	private Thread thread;

	/**
	 * 版本信息
	 * <p>
	 * 调用{@link Object#equals(Object)}方法判断版本是否发生改变，如果发生改变，则启动hotswap。
	 */
	private Object lastVersion;

	@SuppressWarnings("unchecked")
	public HotswapMonitor(NutshellApplication application, HotswapObserver<?> observer, Object root, int interval) {
		this.application = application;
		this.observer = (HotswapObserver<Object>) observer;
		this.rootPackage = root.getClass().getPackageName();
		this.interval = interval;
		this.lastVersion = observer.getNextVersion();
		Objects.requireNonNull(lastVersion, "Version initialization failed.");
	}

	/**
	 * 启动监控
	 */
	public void start() {
		if (!this.running) {
			this.running = true;
			this.thread = new Thread(this, "Hotswap Monitor");
			this.thread.setDaemon(true);
			this.thread.start();
		}
	}

	/**
	 * 停止监控
	 */
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
			try {
				Object nextVersion = observer.getNextVersion();
				if (!lastVersion.equals(nextVersion)) {
					File file = observer.getHotswapFile(nextVersion);
					if (file.isDirectory()) {
						Set<String> subpackages = PackageUtils.findSubpackages(rootPackage);
						try {
							application.hotswap(subpackages.toArray(new String[0]));
							System.gc();
						} catch (Throwable e) {
							e.printStackTrace();
						}
						lastVersion = nextVersion;
					} else {
						// TODO jar file
					}
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(interval);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
