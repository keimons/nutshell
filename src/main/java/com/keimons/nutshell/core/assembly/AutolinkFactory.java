package com.keimons.nutshell.core.assembly;

import com.keimons.nutshell.core.Autolink;
import com.keimons.nutshell.core.Hotswappable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.function.Consumer;

/**
 * 工厂类
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 **/
public class AutolinkFactory {

	/**
	 * 生成一个{@link Autolink}的链接
	 *
	 * @param imports 导入模块
	 * @param exports 导出模块
	 * @param inf     类型
	 * @return 带有链接的对象
	 * @throws Throwable 链接异常
	 */
	public static Object create(Assembly imports, Assembly exports, Class<?> inf) throws Throwable {
		return createProxyInstance(imports, exports, inf);
	}

	/**
	 * 生成一个动态代理的链接
	 *
	 * @param importAssembly 导出模块
	 * @param exportAssembly 导入模块
	 * @param inf            导入导出节点
	 * @return 动态代理的链接
	 * @throws Throwable 链接异常
	 */
	@SuppressWarnings("unchecked")
	private static Object createProxyInstance(Assembly importAssembly, Assembly exportAssembly, Class<?> inf) throws Throwable {
		final String interfaceName = inf.getName();
		final ClassLoader loader = inf.getClassLoader();
		Class<?> proxyClass = loader.loadClass("com.keimons.nutshell.core.assembly.AutolinkProxy");
		Object proxy = proxyClass.getConstructor(String.class).newInstance(interfaceName);
		exportAssembly.registerEvent(EventType.EVENT_HOTSWAP, (params) -> {
			Object instance = exportAssembly.findImplement(interfaceName);
			((Hotswappable) proxy).hotswap(instance);
		});
		exportAssembly.addHotswappable((Hotswappable) proxy);
		exportAssembly.registerEvent(EventType.EVENT_STW, (params) -> ((Hotswappable) proxy).stw((Boolean) params[0], (Consumer<Thread>) params[1]));
		Object instance = exportAssembly.findImplement(interfaceName);
		((Hotswappable) proxy).hotswap(instance);
		return Proxy.newProxyInstance(loader, new Class[]{inf}, (InvocationHandler) proxy);
	}
}
