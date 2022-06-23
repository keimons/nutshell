package com.keimons.nutshell.explorer.test.forgame;

/**
 * 玩家实体
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class Player {

	/**
	 * 玩家唯一ID
	 */
	private String playerId;

	/**
	 * 玩家昵称
	 */
	private String name;

	/**
	 * 玩家经验
	 */
	private int exp;

	/**
	 * 玩家等级
	 */
	private int level;

	/**
	 * 玩家所在组织
	 * <p>
	 * 取值为：{@link String}组织ID或者{@code null}表示没有组织。
	 */
	private String unionId;

	public String getPlayerId() {
		return playerId;
	}

	public void setPlayerId(String playerId) {
		this.playerId = playerId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getExp() {
		return exp;
	}

	public void setExp(int exp) {
		this.exp = exp;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public String getUnionId() {
		return unionId;
	}

	public void setUnionId(String unionId) {
		this.unionId = unionId;
	}
}
