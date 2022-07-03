package com.keimons.nutshell.test.demo;

import java.util.concurrent.TimeoutException;

public class UnionService implements IUnionSharable {

	@Override
	public int getMemberCount(int unionId) {
		return 0;
	}

	@Override
	public int addMember(int unionId, Player player) throws TimeoutException {
		return 0;
	}
}
