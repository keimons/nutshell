package com.keimons.nutshell.explorer.test.forgame;

import com.keimons.deepjson.JsonObject;

/**
 * 组织相关
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
@MsgGroup(opCode = 1000, desc = "组织相关协议", playerFence = true, strategies = UnionHandler.UnionIdFencePolicy.class)
public class UnionHandler {

	@MsgCode(opCode = 1001, desc = "查找组织", strategies = {})
	public void findUnion(Player player, JsonObject json) {

	}

	@MsgCode(opCode = 1002, desc = "加入组织")
	public void joinUnion(Player player, JsonObject json) {

	}

	@MsgCode(opCode = 1003, desc = "退出组织")
	public void exitUnion(Player player, JsonObject json) {

	}

	public static class UnionIdFencePolicy implements FenceStrategy {

		@Override
		public Object getFence(JsonObject json) {
			return json.get("unionId");
		}
	}
}
