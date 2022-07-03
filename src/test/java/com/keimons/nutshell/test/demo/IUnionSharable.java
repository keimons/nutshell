package com.keimons.nutshell.test.demo;

import java.util.concurrent.TimeoutException;

@Remotable
public interface IUnionSharable {

	int getMemberCount(int unionId) throws TimeoutException;

	int addMember(int unionId, Player player) throws TimeoutException;
}
