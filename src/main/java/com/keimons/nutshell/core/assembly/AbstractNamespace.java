package com.keimons.nutshell.core.assembly;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 命名空间
 * <p>
 * {@link Namespace}是内部api不对外公开命名空间中记录{@link Assembly}的：
 * <ul>
 *     <li>classes</li>
 *     <li>class bytes</li>
 *     <li>class loader</li>
 *     <li>export classes</li>
 *     <li>import classes</li>
 * </ul>
 * 命名空间中的所有类使用同一个类装载器。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 **/
public abstract class AbstractNamespace<T extends ClassLoader> implements Namespace {

	final T classLoader;

	final Map<String, byte[]> classBytes;

	final Map<String, Class<?>> classes;

	/**
	 * 导出实例
	 */
	final Map<String, Object> exports;

	protected AbstractNamespace(T classLoader) {
		this.classLoader = classLoader;
		this.classBytes = new HashMap<String, byte[]>();
		this.classes = new HashMap<String, Class<?>>();
		this.exports = new ConcurrentHashMap<String, Object>();
	}

	protected AbstractNamespace(T classLoader, Map<String, byte[]> classBytes, Map<String, Class<?>> classes, Map<String, Object> exports) {
		this.classLoader = classLoader;
		this.classBytes = classBytes;
		this.classes = classes;
		this.exports = exports;
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

	@Override
	public Map<String, byte[]> getClassBytes() {
		return classBytes;
	}
}
