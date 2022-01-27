package com.keimons.nutshell.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ClassLoader
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 9
 **/
public class NutshellClassLoader extends URLClassLoader {

	/**
	 * 类加载器名字
	 */
	private final String name;

	/**
	 * 程序集
	 */
	private String assembly;

	private final Set<String> classNames = new HashSet<String>();

	private Set<String> LOCAL = Collections.singleton("com.keimons.nutshell.core.assembly.DynamicLinkProxy");

	/**
	 * 构造方法
	 *
	 * @param name 类装载器的名字
	 */
	public NutshellClassLoader(String name, String... classNames) {
		super(new URL[]{}, null);
		this.name = name;
		this.classNames.addAll(List.of(classNames));
	}

	/**
	 * 构造方法
	 *
	 * @param name 类装载器的名字
	 */
	public NutshellClassLoader(String name, List<String> classNames) {
		super(new URL[]{}, null);
		this.name = name;
		this.classNames.addAll(classNames);
	}

	/**
	 * 构造方法
	 *
	 * @param parent 父类加载
	 * @param name   类装载器的名字
	 */
	public NutshellClassLoader(ClassLoader parent, String name) {
		super(new URL[]{NutshellClassLoader.class.getResource("")}, parent);
		this.name = name;
	}

	/**
	 * 构造方法
	 *
	 * @param parent 父类加载
	 * @param name   类装载器的名字
	 */
	public NutshellClassLoader(URL[] urls, java.lang.ClassLoader parent, String name) {
		super(urls, parent);
		this.name = name;
	}

	@Override
	protected Class<?> findClass(String className) throws ClassNotFoundException {
		if (!classNames.contains(className) && !LOCAL.contains(className)) {
			return Class.forName(className);
		}
		try {
			String fileName = "/" + className.replaceAll("\\.", "/") + ".class";
			InputStream is = getClass().getResourceAsStream(fileName);
			byte[] b = new byte[is.available()];
			is.read(b);
			return defineClass(className, b, 0, b.length);
		} catch (IOException e) {
			throw new ClassNotFoundException(className);
		}
	}

	@Override
	public String toString() {
		return this.name;
	}

	/**
	 * 定位基于当前上下文的父类加载器
	 *
	 * @return 返回可用的父类加载器.
	 */
	private static ClassLoader findParentClassLoader() {
		ClassLoader parent = NutshellClassLoader.class.getClassLoader();
		if (parent == null) {
			parent = ClassLoader.getSystemClassLoader();
		}
		return parent;
	}

	@Override
	public String getName() {
		return name;
	}

	public Set<String> getClassNames() {
		return classNames;
	}
}
