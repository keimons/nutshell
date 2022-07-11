package com.keimons.nutshell.explorer.test.forgame;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 组织
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 **/
public class Union {

	/**
	 * 组织中的玩家
	 */
	private final Map<String, Player> members = new ConcurrentHashMap<>();

	/**
	 * 获取成员人数
	 *
	 * @return 组织成员人数
	 */
	public int getMemberCount() {
		return members.size();
	}

	/**
	 * 增加一个玩家
	 *
	 * @param player 玩家
	 */
	public void addMember(Player player) {
		members.put(player.getPlayerId(), player);
	}

	public Player getMember(String playerId) {
		return members.get(playerId);
	}

	/**
	 * 移除一个玩家
	 *
	 * @param playerId 玩家ID
	 * @return 是否移除成功
	 */
	public boolean removeMember(String playerId) {
		return members.remove(playerId) != null;
	}
}
