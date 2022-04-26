package com.keimons.nutshell.core;

/**
 * 上下文环境
 * <p>
 * 设计有三级上下文环境，分别是：
 * <ul>
 *     <li>1. Application上下文环境。</li>
 *     <li>2. Session上下文环境</li>
 *     <li>3. Command上下文环境</li>
 * </ul>
 * 三级上下文环境协同，范围逐级缩小。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public interface Context {

}
