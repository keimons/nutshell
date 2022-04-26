package com.keimons.nutshell.core.sequence;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 定序器
 * <p>
 * 为每一个模块生成序列。根本原因是对于{@link Map}的O(1)性能不满意，存储数组可以获得更好的性能。
 * 采用注入的方式，提前查找到模块的下标，运行时注入玩家对应下标位置的对象。{@link MethodHandles}句柄转换：
 * <pre>
 *     消息处理：
 *     void handle(YourselfSession session, YourselfMessage message, Equip equip, Hero hero);
 *     插入参数：
 *     void handle(YourselfSession session, YourselfMessage message, Equip equip, Hero hero, SessionContext context);
 *     假定Hero处于0位置，Equip处于1位置，在context中获取0和1位置，修改参数：
 *     void handle0(YourselfSession session, YourselfMessage message, SessionContext context) {
 *         Hero hero = context.get(0);
 *         Equip equip = context.get(1);
 *         handle(session, message, equip, hero)
 *     }
 * </pre>
 * 此模块设计意义在于，保证每个模块仅有一个序列，通过序列提高查找效率。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class Sequencer {

	/**
	 * 索引生成器
	 */
	private final AtomicInteger indexGenerator = new AtomicInteger();

	/**
	 * 模块对应的下标
	 */
	private final Map<Class<?>, Integer> indexes = new HashMap<>();

	/**
	 * 生成索引
	 * <p>
	 * 确保生成的索引是唯一的。
	 *
	 * @param key 索引生成依据
	 * @return 索引
	 */
	public synchronized int createIndex(Class<?> key) {
		Integer index = indexes.get(key);
		if (index == null) {
			index = indexGenerator.getAndIncrement();
			indexes.put(key, index);
		}
		return index;
	}

	/**
	 * 获取
	 *
	 * @param key {@link Class}对象。或者其它对象。
	 * @return 该对象的下标
	 */
	public int getIndex(Class<?> key) {
		return indexes.getOrDefault(key, -1);
	}
}
