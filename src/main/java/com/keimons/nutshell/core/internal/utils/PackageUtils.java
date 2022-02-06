package com.keimons.nutshell.core.internal.utils;

import com.keimons.nutshell.core.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 读取某一个jar包或者某一个文件夹下的所有class文件
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class PackageUtils {

	public static Set<String> findSubpackages(final String packageName) {
		return findPackages(packageName, 1);
	}

	/**
	 * 从包package中获取所有的Class
	 *
	 * @param pkgName 包名
	 * @param level   层级
	 * @return 该包下所有的class文件
	 */
	public static Set<String> findPackages(final String pkgName, int level) {
		// 第一个class类的集合
		Set<String> classes = new LinkedHashSet<>();
		// 获取包的名字 并进行替换
		String packageName = pkgName;
		String packageDirName = packageName.replace('.', '/');
		// 定义一个枚举的集合 并进行循环来处理这个目录下的things
		Enumeration<URL> dirs;
		try {
			dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
			// 循环迭代下去
			while (dirs.hasMoreElements()) {
				// 获取下一个元素
				URL url = dirs.nextElement();
				// 得到协议的名称
				String protocol = url.getProtocol();
				// 如果是以文件的形式保存在服务器上
				if ("file".equals(protocol)) {
					// 获取包的物理路径
					String filePath = URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8);
					// 以文件的方式扫描整个包下的文件 并添加到集合中
					findAndAddClassesInPackageByFile(pkgName, filePath, classes, level);
				} else if ("jar".equals(protocol)) {
					// 如果是jar包文件
					// 定义一个JarFile
					JarFile jar;
					try {
						// 获取jar
						jar = ((JarURLConnection) url.openConnection()).getJarFile();
						// 从此jar包 得到一个枚举类
						Enumeration<JarEntry> entries = jar.entries();
						// 同样的进行循环迭代
						while (entries.hasMoreElements()) {
							// 获取jar里的一个实体 可以是目录 和一些jar包里的其他文件 如META-INF等文件
							JarEntry entry = entries.nextElement();
							String name = entry.getName();
							// 如果是以/开头的
							if (name.charAt(0) == '/') {
								// 获取后面的字符串
								name = name.substring(1);
							}
							System.out.println(packageDirName);
							// 如果前半部分和定义的包名相同
							if (name.startsWith(packageDirName)) {
								int idx = name.lastIndexOf('/');
								// 如果以"/"结尾 是一个包
								if (idx != -1) {
									// 获取包名 把"/"替换成"."
									packageName = name.substring(0, idx).replace('/', '.');
								}
								// 如果可以迭代下去 并且是一个包
								// 如果是一个.class文件 而且不是目录
								if (name.endsWith(".class") && !entry.isDirectory()/* && (subdirectory || packageName.equals(pkgName))*/) {
									// 去掉后面的".class" 获取真正的类名
									String className = name.substring(packageName.length() + 1, name.length() - 6);
									// 添加到classes
									classes.add(packageName + '.' + className);
								}
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return classes;
	}

	/**
	 * 以文件的形式来获取包下的所有Class
	 *
	 * @param packageName 包名称
	 * @param packagePath 包路径
	 * @param classes     文件
	 * @param level
	 */
	private static void findAndAddClassesInPackageByFile(
			String packageName,
			String packagePath,
			Set<String> classes,
			int level) {
		if (level == 0) {
			classes.add(packageName);
			return;
		}
		level--;
		// 获取此包的目录 建立一个File
		File dir = new File(packagePath);
		// 如果不存在或者 也不是目录就直接返回
		if (!dir.exists() || !dir.isDirectory()) {
			return;
		}
		// 如果存在 就获取包下的所有文件 包括目录
		// 自定义过滤规则 如果可以循环(包含子目录) 或则是以.class结尾的文件(编译好的java类文件)
		File[] files = dir.listFiles(File::isDirectory);
		String directory = StringUtils.isEmpty(packageName) ? "" : packageName + ".";
		// 循环所有文件
		for (File file : Objects.requireNonNull(files)) {
			// 目录 则继续扫描
			findAndAddClassesInPackageByFile(directory + file.getName(), file.getAbsolutePath(), classes, level);
		}
	}
}
