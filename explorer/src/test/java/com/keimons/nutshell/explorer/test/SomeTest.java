package com.keimons.nutshell.explorer.test;

import org.junit.jupiter.api.Test;

import java.util.concurrent.locks.LockSupport;

public class SomeTest {

	@Test
	public void test() {
		Thread thread = Thread.currentThread();
		long startTime = System.currentTimeMillis();
		for (int i = 0; i < 10000000; i++) {
			LockSupport.unpark(thread);
		}
		System.out.println("time0: " + (System.currentTimeMillis() - startTime));

		long t = 0;
		startTime = System.currentTimeMillis();
		for (int i = 0; i < 10000000; i++) {
			t += System.currentTimeMillis();
		}
		System.out.println(t + "time0: " + (System.currentTimeMillis() - startTime));
	}
}
