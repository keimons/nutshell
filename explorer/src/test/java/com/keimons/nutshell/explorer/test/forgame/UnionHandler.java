package com.keimons.nutshell.explorer.test.forgame;

import java.util.HashMap;
import java.util.Map;

/**
 * 组织相关
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
@MsgGroup(opCode = 1000, desc = "组织相关协议")
public class UnionHandler {

	private static final Map<String, Union> unions = new HashMap<>();

	@MsgCode(opCode = 1006, desc = "组织捐献", strategies = PlayerIdAndUnionIdPolicy.class)
	public Object donate(Player player, JsonObject json) {
//		player.cost();
		String unionId = json.getString("unionId");
//		Union unionId = unions.get(json.getString("unionId"));
		Union union = unions.get(unionId);
		int memberCount = union.getMemberCount();
		if (memberCount < 100) {
			union.addMember(player);
			player.setUnionId(unionId);
			return true;
		} else {
			return false;
		}
	}

	public static class PlayerIdAndUnionIdPolicy implements FenceStrategy {

		@Override
		public Object[] getFences(Player player, JsonObject request) {
			Object[] fences = new Object[2];
			fences[0] = player.getPlayerId();
			fences[1] = request.getString("unionId");
			return fences;
		}
	}
}
