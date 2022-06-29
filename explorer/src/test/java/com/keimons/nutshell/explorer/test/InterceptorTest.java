package com.keimons.nutshell.explorer.test;

import com.keimons.nutshell.explorer.Interceptor;
import com.keimons.nutshell.explorer.support.ParkInterceptor;
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
					interceptor.release(0);
				}
			});
			thread.start();
		}
		Thread.sleep(10000);
	}
}
