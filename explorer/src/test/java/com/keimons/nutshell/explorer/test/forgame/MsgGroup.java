package com.keimons.nutshell.explorer.test.forgame;

import java.lang.annotation.*;

/**
 * 定义消息组
 * <p>
 * 消息组的配置。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 **/
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MsgGroup {

	/**
	 * 消息号
	 *
	 * @return 消息号
	 */
	int opCode();

	/**
	 * 消息描述
	 *
	 * @return 消息描述
	 */
	String desc();

	/**
	 * 缺省策略屏障
	 *
	 * @return 消息组的公共屏障策略
	 */
	Class<? extends FenceStrategy>[] strategies() default {};
}
