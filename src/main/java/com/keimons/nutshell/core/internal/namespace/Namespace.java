package com.keimons.nutshell.core.internal.namespace;

import com.keimons.nutshell.core.assembly.Assembly;
import com.keimons.nutshell.core.internal.HotswapClassLoader;

import java.util.Map;

/**
 * 命名空间
 * <p>
 * 为了便于管理，nutshell将{@link Assembly}的所有资源集中在命名空间。
 * 每个命名空间中包含{@link Assembly}的：
 * <ul>
 *     <li>class loader</li>
 *     <li>classes</li>
 *     <li>exports</li>
 * </ul>
 * 命名空间中的所有类使用同一个类装载器装载，依赖类将会尝试使用该类装载器装载，
 * 但依赖类有可能超出该装载器的装载范围。当装载超出命名空间的类时，将使用父类装载器装载。
 * <p>
 * 注意：{@link Namespace}是内部api，不对外公开。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @see HotswapClassLoader 类装载器
 * @since 11
 **/
public interface Namespace {

	/**
	 * 获取命名空间的类装载器
	 *
	 * @return 命名空间的类装载器
	 */
	ClassLoader getClassLoader();

	/**
	 * 划分到该命名空间中的类
	 * <p>
	 * 注意：仅返回命名空间中的类，不包含依赖类。
	 *
	 * @return 命名空间中的类
	 */
	Map<String, Class<?>> getClasses();

	/**
	 * 导出实例
	 * <p>
	 * 设计与普通的设计的不同之处在于nutshell将所有的单例存放在{@link Namespace}中，
	 * 由每个{@link Assembly}单独管理。数据存储为：
	 * <ul>
	 *     <li>key  : interface</li>
	 *     <li>value: instance</li>
	 * </ul>
	 * 所有单例的卸载跟随Assembly一起卸载。
	 *
	 * @return 导出实例
	 */
	Map<String, Object> getExports();
}
