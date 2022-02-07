package com.keimons.nutshell.core.internal.namespace;

import com.keimons.nutshell.core.assembly.Assembly;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 命名空间
 * <p>
 * {@link Namespace}是内部api不对外公开命名空间中记录{@link Assembly}的：
 * <ul>
 *     <li>classes</li>
 *     <li>class loader</li>
 *     <li>export classes</li>
 *     <li>import classes</li>
 * </ul>
 * 命名空间中的所有类使用同一个类装载器。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
public class DefaultNamespace implements Namespace {

	protected final ClassLoader classLoader;

	protected final Map<String, Class<?>> classes;

	protected final Map<String, Object> exports = new ConcurrentHashMap<String, Object>();

	public DefaultNamespace(ClassLoader classLoader, Map<String, Class<?>> classes) {
		this.classLoader = classLoader;
		this.classes = Collections.unmodifiableMap(classes);
	}

	@Override
	public ClassLoader getClassLoader() {
		return classLoader;
	}

	@Override
	public Map<String, Class<?>> getClasses() {
		return classes;
	}

	@Override
	public Map<String, Object> getExports() {
		return exports;
	}
}
