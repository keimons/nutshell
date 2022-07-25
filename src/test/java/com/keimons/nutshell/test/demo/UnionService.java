package com.keimons.nutshell.test.demo;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * 工会服务
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
public class UnionService implements IUnionSharable {

	private final Map<Long, Union> unions = new HashMap<>();

	@Override
	public int getMemberCount(long unionId) {
		Union union = unions.get(unionId);
		if (union == null) {
			throw new RuntimeException();
		}
		return union.getMemberCount();
	}

	@Override
	public void addMember(long unionId, Player player) throws TimeoutException {
		Union union = unions.get(unionId);
		if (union == null) {
			throw new RuntimeException();
		}
		union.addMember(player);
	}
}
