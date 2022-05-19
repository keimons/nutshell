package com.keimons.nutshell.explorer.test.forgame;

import java.lang.annotation.*;

/**
 * 定义消息组
 * <p>
 * 消息组的默认配置。方法级注解覆盖类注解
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MsgGroup {

	int opCode();

	String desc();

	boolean playerFence() default true;

	Class<? extends FenceStrategy>[] strategies() default {};
}
