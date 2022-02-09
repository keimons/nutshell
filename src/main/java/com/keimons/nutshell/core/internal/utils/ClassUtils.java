package com.keimons.nutshell.core.internal.utils;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 读取某一个jar包或者某一个文件夹下的所有class文件
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class ClassUtils {

	/**
	 * Find the implement class of the interface in {@code classes}, ignore the class loader.
	 *
	 * @param classes the classes to search
	 * @param inf     the interface to find
	 * @return the implement class
	 */
	public static Class<?> findImplement(Collection<Class<?>> classes, Class<?> inf) {
		if (!Modifier.isInterface(inf.getModifiers())) {
			String info = "not interface " + inf;
			throw new IllegalArgumentException(info);
		}
		String name = inf.getName();
		for (Class<?> clazz : classes) {
			for (Class<?> ci : clazz.getInterfaces()) {
				if (ci.getName().equalsIgnoreCase(name)) {
					return clazz;
				}
			}
		}
		return null;
	}

	/**
	 * 查找所有注入点
	 * <p>
	 * 在指定的范围内，查找所有标注了该注解的异常。
	 *
	 * @param classes    查找范围
	 * @param annotation 注入点标识
	 * @return 范围内所有的注入点
	 */
	public static Set<Class<?>> findInjections(Collection<Class<?>> classes, Class<? extends Annotation> annotation) {
		Set<Class<?>> injections = new HashSet<Class<?>>();
		for (Class<?> clazz : classes) {
			for (Field field : clazz.getDeclaredFields()) {
				if (field.getAnnotation(annotation) != null) {
					injections.add(field.getType());
				}
			}
		}
		return injections;
	}

	public static boolean hasAnnotation(Class<?> clazz, Class<? extends Annotation> annotation) {
		if (clazz.getAnnotation(annotation) != null) {
			return true;
		}
		for (Field field : clazz.getDeclaredFields()) {
			if (field.getAnnotation(annotation) != null) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 从包package中获取所有的Class
	 *
	 * @param pack         包名
	 * @param subdirectory 是否包含子目录
	 * @return 该包下所有的class文件
	 */
	public static Set<String> findClasses(final String pack, boolean subdirectory) {
		// 第一个class类的集合
		Set<String> classes = new LinkedHashSet<>();
		// 获取包的名字 并进行替换
		String packageName = pack;
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
					findAndAddClassesInPackageByFile(pack, filePath, classes, subdirectory, pack);
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
								if (name.endsWith(".class") && !entry.isDirectory() && (subdirectory || packageName.equals(pack))) {
									// 去掉后面的".class" 获取真正的类名
									String className = name.substring(packageName.length() + 1, name.length() - 6);
									// 添加到classes
									classes.add(className);
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
	 * @param packageName  包名称
	 * @param packagePath  包路径
	 * @param classes      文件
	 * @param subdirectory
	 * @param pack
	 */
	private static void findAndAddClassesInPackageByFile(
			String packageName,
			String packagePath,
			Set<String> classes,
			boolean subdirectory,
			final String pack) {
		// 获取此包的目录 建立一个File
		File dir = new File(packagePath);
		// 如果不存在或者 也不是目录就直接返回
		if (!dir.exists() || !dir.isDirectory()) {
			return;
		}
		// 如果存在 就获取包下的所有文件 包括目录
		// 自定义过滤规则 如果可以循环(包含子目录) 或则是以.class结尾的文件(编译好的java类文件)
		File[] files = dir.listFiles(file -> file.isDirectory() || (file.getName().endsWith(".class")));
		// 循环所有文件
		for (File file : Objects.requireNonNull(files)) {
			// 如果是目录 则继续扫描
			if (file.isDirectory()) {
				if (packageName.equals("")) {
					findAndAddClassesInPackageByFile(file.getName(), file.getAbsolutePath(), classes, subdirectory, pack);
				} else {
					findAndAddClassesInPackageByFile(packageName + "." + file.getName(), file.getAbsolutePath(), classes, subdirectory, pack);
				}
			} else {
				if (subdirectory || pack.equals(packageName)) {
					// 如果是java类文件 去掉后面的.class 只留下类名并添加到集合中
					String className = file.getName().substring(0, file.getName().length() - 6);
					classes.add(packageName + '.' + className);
				}
			}
		}
	}
}
