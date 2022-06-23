package com.keimons.nutshell.explorer.test.forgame;

/**
 * 公共屏障策略
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class CommonStrategy {

	/**
	 * 玩家ID屏障策略
	 */
	public static class PlayerIdPolicy implements FenceStrategy {

		@Override
		public Object getFence(Player player, JsonObject request) {
			return player.getPlayerId();
		}
	}
}
