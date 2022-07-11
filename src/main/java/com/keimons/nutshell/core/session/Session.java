package com.keimons.nutshell.core.session;

import com.keimons.nutshell.core.internal.utils.MethodUtils;

import java.lang.invoke.MethodHandle;

/**
 * Session会话
 * <p>
 * Session是网络通讯、用户数据、消息处理的桥梁。每一个Session有一个唯一标识，它必须和用户标识相同。
 * {@code sessionId}的类型可以是多种多养的，项目偏爱{@code int, long, String}等各种类型的，
 * 仅仅要求实现类中必须有{@code getSessionId}的方法，返回用户的唯一的标识。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
public interface Session<T> {

	void bind(T ctx);

	/**
	 * 获取SessionId的类型
	 *
	 * @param clazz Session实现
	 * @return SessionId的类型，可能是int, long, String等
	 */
	static Class<?> getSessionIdType(Class<? extends Session<?>> clazz) {
		MethodHandle method = MethodUtils.findMethod(clazz, "getSessionId");
		if (method == null) {
			throw new RuntimeException();
		}
		return method.type().returnType();
	}
}
