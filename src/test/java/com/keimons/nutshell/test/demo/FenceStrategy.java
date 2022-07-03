package com.keimons.nutshell.test.demo;

/**
 * 屏障策略
 * <p>
 * 通过玩家或通过{@code request}获取屏障。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public interface FenceStrategy {

	/**
	 * 返回屏障
	 * <p>
	 * 根据玩家或者解析{@code request}
	 *
	 * @param player  玩家
	 * @param request 请求
	 * @return 屏障
	 */
	Object[] getFences(Player player, JsonObject request);
}
