package com.keimons.nutshell.test.demo;

import java.lang.annotation.*;

/**
 * 标注一个方法有可能是在远程执行
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Remotable {

}
