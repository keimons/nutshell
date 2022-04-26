package com.keimons.nutshell.core.command;

import java.lang.annotation.*;

/**
 * 消息处理器
 * <p>
 * 使用String作为命令。Nutshell自动扫描class中的handler方法，并为handle方法注入对象。例如：
 * <pre>
 *     &#64;Handler("/activeCode")
 *     public void ActiveCodeHandler {
 *
 *         public void handle(YourselfSession session, YourselfMessage msg, Hero hero, Equip equip) {
 *             // do something
 *         }
 *     }
 * </pre>
 * 所有上下文环境中的参数可自动注入，顺序可自由调整。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Command
public @interface Handler {

	String value();
}
