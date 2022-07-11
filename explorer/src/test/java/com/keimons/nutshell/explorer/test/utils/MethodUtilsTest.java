package com.keimons.nutshell.explorer.test.utils;

import com.keimons.nutshell.explorer.InvokeException;
import com.keimons.nutshell.explorer.utils.MethodUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.concurrent.TimeoutException;

/**
 * {@link MethodUtils}工具类测试
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
public class MethodUtilsTest {

	@Test
	public void test() throws NoSuchMethodException {
		Method method_test0 = MethodUtilsTest.class.getDeclaredMethod("test0");
		Assertions.assertTrue(MethodUtils.checkThrows(method_test0, Throwable.class));
		Assertions.assertTrue(MethodUtils.checkThrows(method_test0, Exception.class));
		Assertions.assertFalse(MethodUtils.checkThrows(method_test0, RuntimeException.class));

		Method method_test1 = MethodUtilsTest.class.getDeclaredMethod("test1");
		Assertions.assertTrue(MethodUtils.checkThrows(method_test1, Exception.class));
		Assertions.assertTrue(MethodUtils.checkThrows(method_test1, TimeoutException.class));
		Assertions.assertTrue(MethodUtils.checkThrows(method_test1, NullPointerException.class));
		Assertions.assertTrue(MethodUtils.checkThrows(method_test1, TimeoutException.class, NullPointerException.class));
		Assertions.assertTrue(MethodUtils.checkThrows(method_test1, Exception.class, TimeoutException.class, NullPointerException.class));
		Assertions.assertTrue(MethodUtils.checkThrows(method_test1, RuntimeException.class));
		Assertions.assertFalse(MethodUtils.checkThrows(method_test1, IndexOutOfBoundsException.class));

		Method method_test2 = MethodUtilsTest.class.getDeclaredMethod("test2");
		Assertions.assertTrue(MethodUtils.checkThrows(method_test2, InvokeException.class, TimeoutException.class));

		try {
			test2();
		} catch (InvokeException | TimeoutException e) {
			e.printStackTrace();
		}
	}

	private void test0() throws Exception {
		// do nothing
	}

	private void test1() throws TimeoutException, NullPointerException {
		// do nothing
	}

	private void test2() throws InvokeException, TimeoutException {
		// do nothing
	}
}
