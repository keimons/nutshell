package com.keimons.nutshell.explorer.test.forgame;

@MsgGroup(opCode = 10000, desc = "世界地图相关协议")
public class WorldHandler {

	@MsgCode(opCode = 10001, desc = "迁城", strategies = PlayerIdAndMoveIdPolicy.class)
	public void moveCity(Player player, JsonObject request) {
		// move city
	}

	@MsgCode(opCode = 10002, desc = "驻扎", strategies = PlayerIdAndPointIdPolicy.class)
	public void stationed(Player player, JsonObject request) {
		// stationed point
	}

	public static class PlayerIdAndMoveIdPolicy implements FenceStrategy {

		@Override
		public Object[] getFences(Player player, JsonObject request) {
			int x = request.getInt("x"), y = request.getInt("y");
			return new Object[]{player.getPlayerId(), // 附带落地4个格子的坐标
					new Point(x, y), new Point(x, y + 1),
					new Point(x + 1, y), new Point(x + 1, y + 1)
			};
		}
	}

	public static class PlayerIdAndPointIdPolicy implements FenceStrategy {

		@Override
		public Object[] getFences(Player player, JsonObject request) {
			int x = request.getInt("x"), y = request.getInt("y");
			return new Object[]{player.getPlayerId(), new Point(x, y)};
		}
	}
}

record Point(int x, int y) {
}
