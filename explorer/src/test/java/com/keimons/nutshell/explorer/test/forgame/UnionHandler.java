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
@MsgGroup(opCode = 1000, desc = "组织相关协议", strategies = {CommonStrategy.PlayerIdPolicy.class})
public class UnionHandler {

	private static final Map<String, Union> unions = new HashMap<>();

	@MsgCode(opCode = 1001, desc = "查找组织")
	public Object findUnion(Player player, JsonObject json) {
		return unions;
	}

	@MsgCode(opCode = 1002, desc = "加入组织", attach = UnionIdFencePolicy.class)
	public Object joinUnion(Player player, JsonObject json) {
		String unionId = json.getString("unionId");
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

	@MsgCode(opCode = 1003, desc = "踢出组织", attach = {UnionIdByPlayerFencePolicy.class, TargetIdFencePolicy.class})
	public Object exitUnion(Player player, JsonObject json) {
		String unionId = player.getUnionId();
		String targetId = json.getString("targetId");
		Union union = unions.get(unionId);
		if (union != null) {
			Player member = union.getMember(targetId);
			member.setUnionId(null);
			return union.removeMember(targetId);
		} else {
			return false;
		}
	}

	@MsgCode(opCode = 1004, desc = "处理加入组织", cover = {UnionIdByPlayerFencePolicy.class, TargetIdFencePolicy.class})
	public Object handleJoinUnion(Player player, JsonObject json) {
		return null;
	}

	public static class UnionIdFencePolicy implements FenceStrategy {

		@Override
		public Object getFence(Player player, JsonObject request) {
			return request.getString("unionId");
		}
	}

	public static class UnionIdByPlayerFencePolicy implements FenceStrategy {

		@Override
		public Object getFence(Player player, JsonObject request) {
			// check
			return player.getUnionId();
		}
	}

	public static class TargetIdFencePolicy implements FenceStrategy {

		@Override
		public Object getFence(Player player, JsonObject request) {
			return request.getString("targetId");
		}
	}
}
