package com.keimons.nutshell.test.demo;

import java.util.concurrent.TimeoutException;

/**
 * 工会分享模块
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
@Remotable
public interface IUnionSharable {

	/**
	 * 获取帮派人数
	 *
	 * @param unionId 帮派唯一ID
	 * @return 帮派人数
	 * @throws TimeoutException 异步执行情况下，可能发生超时。
	 */
	int getMemberCount(long unionId) throws TimeoutException;

	/**
	 * 增加帮派人员
	 *
	 * @param unionId 帮派唯一ID
	 * @param player  要加入帮派的玩家
	 * @throws TimeoutException 异步执行情况下，可能发生超时。
	 */
	void addMember(long unionId, Player player) throws TimeoutException;
}
