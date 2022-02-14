package com.keimons.nutshell.core.assembly;

import com.keimons.nutshell.core.Hotswappable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 自动链接的代理
 * <p>
 * 注入的对象，并不是真正的对象，而是对象的一个代理。
 * 尽管看似是相同的两个对象，但由于类装载器的不同，会造成{@code com.keimons.nutshell.Object}不等于
 * {@code com.keimons.nutshell.Object}。因为它们不属于同一个类装载器。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
public class AutolinkProxy implements Hotswappable, InvocationHandler {

	private final String interfaceName;

	/**
	 * 原子更新注入的对象和方法
	 */
	private Node node;

	private Node back;

	public AutolinkProxy(String interfaceName) {
		this.interfaceName = interfaceName;
	}

	@Override
	public void hotswap(Object instance) throws Throwable {
		Map<String, Method> methods = new HashMap<>();
		for (Method method : instance.getClass().getInterfaces()[0].getDeclaredMethods()) {
			methods.put(method.getName(), method);
		}
		this.back = node;
		this.node = new Node(instance, methods);
	}

	@Override
	public void rollback() {
		if (back != null) {
			node = back;
			back = null;
		}
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Node node = this.node;
		Object module = node.instance;
		Method newMethod = node.methods.get(method.getName());
		return Objects.requireNonNullElse(newMethod, method).invoke(module, args);
	}

	/**
	 * 防止多个字段不能原子更新
	 */
	private static class Node {

		/**
		 * 真实的引用的对象
		 */
		private final Object instance;

		/**
		 * 方法映射
		 */
		private final Map<String, Method> methods;

		public Node(Object instance, Map<String, Method> methods) {
			this.instance = instance;
			this.methods = methods;
		}
	}
}
