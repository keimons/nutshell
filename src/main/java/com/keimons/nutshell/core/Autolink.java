package com.keimons.nutshell.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Proxy;

/**
 * 自动链接
 * <p>
 * 取链接之意，并不是简单的注入，它更像是链接两个模块的适配器。
 * <pre>
 * +----------+       +----------+       +----------+
 * | Module A | ----> | Autolink | ----> | Module B |
 * +----------+       +----------+       +----------+
 * </pre>
 * 自动链接是注入的一种，它是系统级别的注入。它的概念等价于桥接和适配器，存在于模块和模块之间的。
 * 自动链接生成一个指向{@code Module B}的代理，通过代理进行对于{@code Module B}的访问，
 * 代理的可选实现：
 * <ul>
 *     <li>{@link Proxy}动态代理，性能很低</li>
 * </ul>
 * 自动链接要求使用{@code Module A}的类装载器进行装载，与{@code Module A}拥有相同的生命
 * 周期。自动链接要求有版本的概念，当{@code Module B}更新时，安装新的{@code Module B}到
 * 系统中。此时，系统中存在两个{@code Module B}，安装完成后，开始链接，链接成功后，才能进行
 * 模块升级。当{@code Module B}升级完成后，卸载旧的模块。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 9
 **/
@Injectable
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Autolink {

	/**
	 * 注入点是否必须注入
	 *
	 * @return 注入点是否必须有实现，默认为{@code true}必须的
	 */
	boolean required() default true;
}
