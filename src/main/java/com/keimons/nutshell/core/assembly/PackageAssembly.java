package com.keimons.nutshell.core.assembly;

import com.keimons.nutshell.core.internal.HotswapClassLoader;
import com.keimons.nutshell.core.internal.utils.ClassUtils;
import com.keimons.nutshell.core.internal.utils.EqualsUtils;
import com.keimons.nutshell.core.internal.utils.FileUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * DefaultAssembly
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class PackageAssembly extends AbstractAssembly<PackageNamespace> {

	public PackageAssembly(String name, PackageNamespace namespace) {
		super(name, namespace);
	}

	/**
	 * hotswap
	 *
	 * @return {@code true}执行hotswap，{@code false}跳过hotswap
	 * @throws IOException hotswap异常
	 */
	public boolean fork() throws IOException {
		String subpackage = namespace.getSubpackage();
		String root = namespace.getRoot();
		Set<String> classNames = ClassUtils.findClasses(subpackage, true);
		Map<String, byte[]> oldCache = namespace.getClassBytes();
		Map<String, byte[]> newCache = new HashMap<String, byte[]>(classNames.size());
		if (oldCache.size() == classNames.size()) {
			for (String className : classNames) {
				newCache.put(className, FileUtils.readClass(className));
			}
		}
		if (EqualsUtils.isEquals(oldCache, newCache)) {
			System.out.println("[hotswap: N]: " + name);
			return false;
		}
		System.out.println("[hotswap: Y]: " + name);
		ClassLoader parent = namespace.getParentClassLoader();
		HotswapClassLoader newLoader = new HotswapClassLoader(name, parent, root);
		PackageNamespace namespace = new ForkPackageNamespace(newLoader, this.namespace);
		for (Map.Entry<String, byte[]> entry : newCache.entrySet()) {
			String className = entry.getKey();
			byte[] bytes = entry.getValue();
			Class<?> clazz = newLoader.loadClass(entry.getKey(), bytes);
			namespace.getClassBytes().put(className, bytes);
			namespace.getClasses().put(className, clazz);
		}
		this.namespace = namespace;
		return true;
	}

	public void join(boolean success) {
		if (success) {
			namespace = new PackageNamespace(namespace);
		} else {
			namespace = namespace.getBackup();
		}
	}
}
