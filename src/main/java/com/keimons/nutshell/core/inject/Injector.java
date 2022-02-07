package com.keimons.nutshell.core.inject;

import com.keimons.nutshell.core.ApplicationContext;
import com.keimons.nutshell.core.Autolink;
import com.keimons.nutshell.core.assembly.Assembly;
import com.keimons.nutshell.core.assembly.AutolinkFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.Objects;

/**
 * 注入器
 * <p>
 * 注入的过程涉及三个部分：
 * <ul>
 *     <li>注入器：执行注入的工具，既本对象</li>
 *     <li>注入点：等待被注入的字段</li>
 *     <li>注入对象：在上下文中查找到的被注入到注入点的对象</li>
 * </ul>
 * <p>
 * 注入器是注入的最小单元，负责对于注入点的注入。执行注入时，在山下文环境中获取到注入对象，
 * 并将其注入到注入点所在对象中。
 * 注入时，如果在上下文环境中没能获取到对象，那么有可能注入为空，这并不会抛出异常。
 * <p>
 * 特别声明：
 * 注入器仅仅负责注入的逻辑，可用于任何注入点的注入，真实的对象查找逻辑是在上下文环境中完成的。
 * 注入器类可以由任意类装载器装载，但是，注入器中有对于原始类型的引用，模块更新时，
 * 需要移除原有的注入器，才能保证旧模块的{@link Class}被卸载。
 * <p>
 * 注意：如果注入点是{@code final}修饰的字段，那么则有可能注入失败。
 * <p>
 * 注意：此为内部API，对外的开放仅限于{@link Injectors}。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @see Autolink 自动链接
 * @since 11
 **/
class Injector {

	/**
	 * 注入类型
	 * <p>
	 * 请悉知：注入器缓存了注入类型，当模块更新时，应该移除模块中的注入器并重新生成，
	 * 否则由不同的类装载器装载的{@link Class}将同时存在。注入类型用于查找注入对象，
	 * 查找注入对象时，应该根据注入类型生成"不动点"进行查找，而不是直接使用注入类型。
	 */
	private final Class<?> injectType;

	/**
	 * 对注入点进行注入的方法句柄
	 * <p>
	 * 它很简单，等价于一个{@code set}操作。句柄的执行：injector.invoke(bean, value);
	 */
	private final MethodHandle injector;

	/**
	 * 根据注入点构造一个注入器
	 *
	 * @param field 注入点
	 * @throws IllegalAccessException 权限异常
	 */
	public Injector(Field field) throws Exception {
		injectType = field.getType();
		injector = MethodHandles.lookup().unreflectSetter(field);
	}

	/**
	 * 注入
	 * <p>
	 * 在上下文环境中，根据注入类型获取注入对象并将其注入到注入点，如果注入对象不存在，则注入{@link null}。
	 * <p>
	 * 注意：当模块迭代时，应该优先注入新模块。
	 *
	 * @param context  上下文环境
	 * @param imports  导入
	 * @param instance 被注入的对象
	 * @throws Throwable 注入失败
	 */
	public void inject(ApplicationContext context, Assembly imports, Object instance) throws Throwable {
		Objects.requireNonNull(instance);
		Assembly exports = context.findInstance(injectType.getName());
		Object proxy = AutolinkFactory.create(imports, exports, injectType);
		injector.invoke(instance, proxy);
	}
}
