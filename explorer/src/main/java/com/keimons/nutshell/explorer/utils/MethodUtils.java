package com.keimons.nutshell.explorer.utils;

import java.lang.reflect.Method;

/**
 * 方法工具类
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
public class MethodUtils {

	/**
	 * 检查一个方法是否抛出预期的异常或预期异常的子异常
	 * <p>
	 * 该方法必须要抛出指定的异常，预期的方法调用：
	 * <pre>{@code
	 * try {
	 *     doRemoteCall(); // 调用远程方法
	 * } catch (InvokeException | TimeoutException e) {
	 *     // do something
	 * }
	 * }</pre>
	 * 针对于抛出必检异常的方法，需要在方法使用时处理有可能出现的异常，不推荐将异常继续向上抛。
	 *
	 * @param method  检查的方法
	 * @param expects 预期的异常
	 * @return {@code true} 抛出预期的异常；<p>{@code false} 未抛出预期的异常。
	 */
	@SuppressWarnings("rawtypes")
	public static boolean checkThrows(Method method, Class... expects) {
		Class<?>[] exceptions = method.getExceptionTypes();
		start:
		for (Class<?> expect : expects) {
			for (Class<?> exception : exceptions) {
				if (expect.isAssignableFrom(exception)) {
					break start;
				}
			}
			return false;
		}
		return true;
	}
}
