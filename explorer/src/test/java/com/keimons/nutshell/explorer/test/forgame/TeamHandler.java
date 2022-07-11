package com.keimons.nutshell.explorer.test.forgame;

import java.util.HashMap;
import java.util.Map;

/**
 * 队伍相关
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 **/
@MsgGroup(opCode = 2000, desc = "组队相关协议")
public class TeamHandler {

	private static final Map<String, Team> teams = new HashMap<>();

	@MsgCode(opCode = 2001, desc = "加入队伍", strategies = PlayerIdAndTeamIdPolicy.class)
	public Object join(Player player, JsonObject json) {
		String teamId = json.getString("teamId");
		Team team = teams.get(teamId);
		int memberCount = team.getMemberCount();
		if (memberCount < 100) {
			team.addMember(player);
			player.setTeamId(teamId);
			return true;
		} else {
			return false;
		}
	}


	public static class PlayerIdAndTeamIdPolicy implements FenceStrategy {

		@Override
		public Object[] getFences(Player player, JsonObject request) {
			Object[] fences = new Object[2];
			fences[0] = player.getPlayerId();
			fences[1] = request.getString("teamId");
			return fences;
		}
	}
}
