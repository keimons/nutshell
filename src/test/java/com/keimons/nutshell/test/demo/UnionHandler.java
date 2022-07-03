package com.keimons.nutshell.test.demo;

import com.keimons.nutshell.core.Autolink;

import java.util.concurrent.TimeoutException;

@MsgGroup(opCode = 1000, desc = "组织相关协议")
public class UnionHandler {

	// 查找并注入一个UnionSharable的动态代理实现，也许是本地，也许是远程呦~
	@Autolink
	public IUnionSharable sharable; // 注入的是代理，并不是真实对象

	@MsgCode(opCode = 1001, desc = "加入组织", strategies = PlayerIdAndUnionIdPolicy.class)
	public void join(Player player, JsonObject request) throws TimeoutException {
		int unionId = request.getInt("unionId");
		// 远程执行 强制抛出必检的TimeoutException
		int memberCount = sharable.getMemberCount(unionId);
		if (memberCount < 100) {
			sharable.addMember(unionId, player);
		}
	}
}

class PlayerIdAndUnionIdPolicy implements FenceStrategy {

	@Override
	public Object[] getFences(Player player, JsonObject request) {
		Object[] fences = new Object[2];
		fences[0] = player.getPlayerId();
		fences[1] = request.getString("unionId");
		return fences;
	}
}
