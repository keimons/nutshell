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

	boolean playerFence() default true;

	Class<? extends FenceStrategy>[] strategies() default {};
}
