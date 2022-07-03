package com.keimons.nutshell.explorer.test.demo;

import com.keimons.nutshell.explorer.test.forgame.FenceStrategy;
import com.keimons.nutshell.explorer.test.forgame.JsonObject;
import com.keimons.nutshell.explorer.test.forgame.Player;

/**
 * 帮派相关执行屏障
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 **/
public class UnionFence implements FenceStrategy {

	/**
	 * 强制使用<b>0线程</b>和<b>1线程</b>处理帮派相关任务
	 */
	private static final int UNION_N_THREAD = 2;

	@Override
	public Object[] getFences(Player player, JsonObject request) {
		return new Object[]{
				player.getPlayerId(),
				request.getInt("unionId") % UNION_N_THREAD
		};
	}
}
