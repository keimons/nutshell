package com.keimons.nutshell.explorer.test.demo;

import com.keimons.nutshell.explorer.test.forgame.Player;
import com.keimons.nutshell.explorer.test.forgame.*;

/**
 * 礼包码相关协议
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
@MsgGroup(opCode = 4000, desc = "礼包码相关协议")
public class GiftCodeHandler {

	// dispatch = true 派遣子线程池执行任务
	@MsgCode(opCode = 4001, dispatch = true, desc = "使用礼包码", strategies = PlayerIdAndGiftIdPolicy.class)
	public Object use(Player player, JsonObject json) {
		String codeId = json.getString("codeId");
		// remote check gift code.
		Object rewards = new Object();
		return rewards;
	}

	public static class PlayerIdAndGiftIdPolicy implements FenceStrategy {

		@Override
		public Object[] getFences(Player player, JsonObject request) {
			Object[] fences = new Object[2];
			fences[0] = player.getPlayerId();
			fences[1] = request.getString("codeId");
			return fences;
		}
	}
}
