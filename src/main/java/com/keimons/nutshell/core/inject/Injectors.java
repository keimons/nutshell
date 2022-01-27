package com.keimons.nutshell.core.inject;

import com.keimons.nutshell.core.Autolink;
import com.keimons.nutshell.core.Context;
import com.keimons.nutshell.core.assembly.Assembly;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 注入工具
 * <p>
 * 这是一个包装容器，记录了一个{@link Class}中所有注入点所对应的注入器。
 * 根据一个{@link Class}中所有的注入点，生成一个注入集合。注入是针对于一个对象的所有字段进行注入，
 * 不会单独的给一个字段进行注入。例如：
 * <pre>
 *     public class Adapter {
 *         &#64;DynamicLink
 *         PlayerSharable player;
 *         &#64;DynamicLink
 *         EquipSharable equip;
 *     }
 * </pre>
 * {@code Adapter}中有两个注入点，当对这个对象进行注入时，从上下文的环境中，获取这两个注入点的实例，
 * 并注入到这个对象中。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @see Autolink 自动链接
 * @since 9
 **/
public class Injectors {

	private final Class<?> entityType;

	/**
	 * 所有注入器
	 * <p>
	 * 根据{@link Class}中所有的注入点生成的注入器。
	 */
	private final List<Injector> injectors;

	/**
	 * 生成{@link Class}的所有注入器
	 * <p>
	 * 尽管{@link Class}中没有任何注入点，也可以正确运行，但是，涉及到类的卸载，
	 * 我们不允许没有注入点的{@link Class}存在。
	 *
	 * @param entityType 拥有注入点的类
	 * @param injectors  类中所有注入点
	 */
	public Injectors(Class<?> entityType, List<Injector> injectors) {
		this.entityType = entityType;
		this.injectors = injectors;
	}

	/**
	 * 生成一个注入集合
	 *
	 * @param clazz       拥有注入点的类
	 * @param annotations 扫描的注入类型
	 * @return 注入器集合
	 * @throws Exception 注入器集合生成异常
	 */
	public static Injectors of(Class<?> clazz, Class<? extends Annotation>... annotations) throws Exception {
		Objects.requireNonNull(clazz);
		Objects.requireNonNull(annotations);
		if (annotations.length <= 0) {
			throw new IllegalStateException();
		}
		List<Injector> injectors = new ArrayList<Injector>();
		for (Field field : clazz.getDeclaredFields()) {
			int count = 0;
			for (Class<? extends Annotation> annotation : annotations) {
				if (field.getAnnotation(annotation) != null) {
					injectors.add(new Injector(field));
					count++;
				}
			}
			if (count > 1) {
				throw new IllegalStateException("multi inject annotation in " + field);
			}
		}
		if (injectors.size() <= 0) {
			throw new IllegalStateException("inject not find in " + clazz);
		}
		injectors = Collections.unmodifiableList(injectors);
		return new Injectors(clazz, injectors);
	}

	/**
	 * 对一个{@code entity}进行注入
	 *
	 * @param context  上下文环境
	 * @param imports  导入模块
	 * @param instance 对其中所有的注入点进行注入
	 * @throws Throwable 注入失败
	 */
	public void inject(Context context, Assembly imports, Object instance) throws Throwable {
		for (Injector injector : injectors) {
			injector.inject(context, imports, instance);
		}
	}
}
