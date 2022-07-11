package com.keimons.nutshell.core.monitor;

import com.keimons.nutshell.core.NutshellApplication;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;

/**
 * FileListener
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
public class FileListener extends FileAlterationListenerAdaptor {

	private final NutshellApplication application;

	private String dir;

	private String pkg;

	public FileListener(NutshellApplication application, String dir, String pkg) {
		this.application = application;
		this.dir = dir;
		this.pkg = pkg;
	}

	/**
	 * 文件创建执行
	 */
	@Override
	public void onFileCreate(File file) {
//		System.out.println("create file:" + file.getAbsolutePath());
		String absolutePath = file.getAbsolutePath();
		String substring = absolutePath.substring(dir.length(), absolutePath.indexOf(File.separator, dir.length()));
		String subpackage = pkg + "." + substring;
		try {
			application.hotswap(subpackage);
			System.gc();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	/**
	 * 文件创建修改
	 */
	@Override
	public void onFileChange(File file) {
//		System.out.println("modify file:" + file.getAbsolutePath());
	}

	/**
	 * 文件删除
	 */
	@Override
	public void onFileDelete(File file) {
//		System.out.println("delete file:" + file.getAbsolutePath());
	}

	/**
	 * 目录创建
	 */
	@Override
	public void onDirectoryCreate(File directory) {
//		System.out.println("create dir:" + directory.getAbsolutePath());
	}

	/**
	 * 目录修改
	 */
	@Override
	public void onDirectoryChange(File directory) {
//		System.out.println("modify dir:" + directory.getAbsolutePath());
	}

	/**
	 * 目录删除
	 */
	@Override
	public void onDirectoryDelete(File directory) {
//		System.out.println("delete dir:" + directory.getAbsolutePath());
	}

	@Override
	public void onStart(FileAlterationObserver observer) {
		super.onStart(observer);
	}

	@Override
	public void onStop(FileAlterationObserver observer) {
		super.onStop(observer);
	}
}
