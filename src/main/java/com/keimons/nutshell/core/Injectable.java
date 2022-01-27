package com.keimons.nutshell.core;

import java.lang.annotation.*;

/**
 * 可注入的
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 9
 **/
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Injectable {

}
