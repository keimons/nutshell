package com.keimons.nutshell.disruptor.test;

import com.keimons.nutshell.disruptor.Interceptor;
import com.keimons.nutshell.disruptor.support.ParkInterceptor;
import org.junit.jupiter.api.Test;

/**
 * InterceptorTest
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class InterceptorTest {

	@Test
	public void test() throws InterruptedException {
		Interceptor interceptor = new ParkInterceptor();
		interceptor.init(3);
		for (int i = 0; i < 4; i++) {
			Thread thread = new Thread(() -> {
				if (interceptor.intercept()) {
					System.out.println("跳过任务");
				} else {
					System.out.println("执行任务");
					interceptor.release();
				}
			});
			thread.start();
		}
		Thread.sleep(10000);
	}
}
