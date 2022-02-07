package com.keimons.nutshell.core;

/**
 * 热插拔的
 * <p>
 * nutshell最核心功能。nutshell注入的对象，并不是真正的对象，而是对象的一个代理。方法调用：
 * <ul>
 *     <li>首先，调用类调用代理对象方法</li>
 *     <li>然后，代理对象调用真实对象方法</li>
 *     <li>最后，逐级返回返回值</li>
 * </ul>
 * 可选的实现有动态代理、ASM编程生成中间对象等。
 * 正由于代理对象的存在，热插拔关联Assembly时，只需要更新代理中的对象既可以。我们打破了传统的认知：
 * <ul>
 *     <li>final字段也是可以更新的。</li>
 *     <li>每个Assembly使用一个单独的类装载器，造成{@code new com.keimons.Object();}无法赋值给{@code private com.keimons.Object value;}</li>
 * </ul>
 * 尽管看似是相同的两个对象，但由于类装载器的不同，会造成{@code com.keimons.Object}不等于
 * {@code com.keimons.Object}。因为它们不属于同一个命名空间中，所以只能通过添加“桥”的方式链接两个模块并且更新被引用模块。
 * <p>
 * Ps: nutshell通过添加监听器，实现Assembly更新时，相关“桥”同时更新。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
public interface Hotswappable {

	/**
	 * 热插拔
	 *
	 * @param instance 模块
	 * @throws Throwable 安装异常
	 */
	void hotswap(Object instance) throws Throwable;
}
