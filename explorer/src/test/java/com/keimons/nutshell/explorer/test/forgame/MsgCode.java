package com.keimons.nutshell.explorer.test.forgame;

import java.lang.annotation.*;

/**
 * 定义消息号
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 **/
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MsgCode {

	/**
	 * 返回消息号
	 * <p>
	 * 消息号是客户端服务器通讯的唯一标识，通过消息号完成客户端与服务器的交互。
	 *
	 * @return 返回消息号
	 */
	int opCode();

	/**
	 * 派发执行
	 * <p>
	 * 由于没有协程，暂时使用结合复合线程池共同执行任务。
	 *
	 * @return {@code true}. 使用子线程池执行；{@code false}. 使用主线程池执行。
	 */
	boolean dispatch() default false;

	/**
	 * 返回消息描述
	 *
	 * @return 返回这个任务是用来做什么的。
	 */
	String desc() default "";

	/**
	 * 附加屏障策略
	 *
	 * @return 屏障策略
	 * @see MsgGroup#strategies() 覆盖消息组的默认策略
	 */
	Class<? extends FenceStrategy> strategies();
}
