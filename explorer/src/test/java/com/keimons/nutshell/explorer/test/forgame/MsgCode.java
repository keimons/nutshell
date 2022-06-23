package com.keimons.nutshell.explorer.test.forgame;

import java.lang.annotation.*;

/**
 * 定义消息号
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MsgCode {

	int opCode();

	String desc() default "";

	/**
	 * 附加屏障策略
	 *
	 * @return 附加的屏障策略
	 * @see MsgGroup#strategies() 再此的基础上，附加一些屏障
	 */
	Class<? extends FenceStrategy>[] attach() default {};

	/**
	 * 覆盖屏障策略
	 * <p>
	 * 使用此屏障策略，覆盖所有配置。
	 *
	 * @return 屏障策略
	 * @see MsgGroup#strategies() 将被覆盖
	 */
	Class<? extends FenceStrategy>[] cover() default {};
}
