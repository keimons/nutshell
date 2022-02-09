package com.keimons.nutshell.core.internal.utils;

import java.io.*;

/**
 * FileUtils
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class FileUtils {

	public static String readLastNotEmptyLine(File file) throws Exception {
		if (!file.exists() || file.isDirectory() || !file.canRead()) {
			return null;
		}
		try (RandomAccessFile rf = new RandomAccessFile(file, "r")) {
			long len = rf.length();
			long start = rf.getFilePointer();
			long nextend = start + len - 1;
			String line;
			rf.seek(nextend);
			int c;
			while (nextend > start) {
				c = rf.read();
				if (c == '\n' || c == '\r') {
					line = rf.readLine();
					if (!line.trim().isEmpty()) {
						return line;
					}
					nextend--;
				}
				nextend--;
				rf.seek(nextend);
				if (nextend == 0) {// 当文件指针退至文件开始处，输出第一行
					return rf.readLine();
				}
			}
		}
		return null;
	}

	public static byte[] readClass(String className) throws IOException {
		String fileName = "/" + className.replaceAll("\\.", "/") + ".class";
		InputStream is = FileUtils.class.getResourceAsStream(fileName);
		if (is == null) {
			throw new FileNotFoundException("file: " + className);
		}
		byte[] bytes = new byte[is.available()];
		is.read(bytes);
		return bytes;
	}
}
