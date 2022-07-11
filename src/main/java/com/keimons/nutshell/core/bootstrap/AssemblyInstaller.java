package com.keimons.nutshell.core.bootstrap;

import com.keimons.nutshell.core.ApplicationContext;
import com.keimons.nutshell.core.Autolink;
import com.keimons.nutshell.core.assembly.Assembly;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * {@link Assembly}的安装和热插拔引导
 * <p>
 * Assembly的安装和热插拔将由引导程序引导完成，安装引导之前，需要{@link ApplicationContext}识别所有Assembly。
 * Assembly之间通过自动注入，使用{@link Autolink}链接，安装和热插拔时。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
public class AssemblyInstaller {

	/**
	 * 安装引导步骤
	 */
	private final LinkedHashMap<String, Bootstrap> bootstraps = new LinkedHashMap<String, Bootstrap>();

	public void addLast(String name, Bootstrap bootstrap) {
		bootstraps.put(name, bootstrap);
	}

	public void install(ApplicationContext context, Assembly... assemblies) throws Throwable {
		install(context, List.of(assemblies));
	}

	public void install(ApplicationContext context, List<Assembly> assemblies) throws Throwable {
		assemblies.forEach(context::add);
		for (Bootstrap bootstrap : bootstraps.values()) {
			for (Assembly assembly : assemblies) {
				bootstrap.install(context, assembly);
			}
		}
	}

	/**
	 * 热插拔
	 * <p>
	 * nutshell的核心功能，安装/更新/卸载一个Assembly。
	 * <p>
	 * 注意：热插拔失败时需要回滚状态。
	 *
	 * @param context    上下文环境
	 * @param assemblies 正在热插拔的模块
	 * @throws Throwable 热插拔中的异常
	 */
	public void hotswap(ApplicationContext context, Assembly... assemblies) throws Throwable {
		hotswap(context, List.of(assemblies));
	}

	/**
	 * 热插拔
	 * <p>
	 * nutshell的核心功能。
	 * <ul>
	 *     <li>安装：nutshell允许运行时添加一个从未出现过的模块。</li>
	 *     <li>更新：nutshell允许运行时更新一个现有Assembly的所有{@link Class}。</li>
	 *     <li>卸載：nutshell允许运行时卸载一个现有Assembly的。</li>
	 * </ul>
	 * <p>
	 * 注意：热插拔失败时需要回滚状态。
	 *
	 * @param context    上下文环境
	 * @param assemblies 正在热插拔的模块
	 * @throws Throwable 热插拔中的异常
	 */
	public void hotswap(ApplicationContext context, List<Assembly> assemblies) throws Throwable {
		assemblies.forEach(context::add);
		List<Assembly> inbound;
		List<Assembly> outbound = assemblies;
		for (Bootstrap bootstrap : bootstraps.values()) {
			inbound = outbound;
			outbound = new ArrayList<Assembly>();
			bootstrap.hotswap(context, inbound, outbound);
		}
	}
}
