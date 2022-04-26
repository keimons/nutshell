package com.keimons.nutshell.disruptor.test;

import org.junit.jupiter.api.Test;

/**
 * LuaTest
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class LuaTest {

	@Test
	public void test() {
		String input = "i am so happy";
		char[] array = input.toCharArray();
		int length = array.length;
		char[] result = new char[length];
		int mark = length - 1;
		int writeIndex = 0;
		for (int i = length - 1; i >= 0; i--) {
			if (array[i] == ' ' || i == 0) {
				int start = i == 0 ? 0 : i + 1;
				int limit = mark == 0 ? 1 : mark - i;
				for (int j = 0; j < limit; j++) {
					result[writeIndex++] = array[start + j];
				}
				mark = i - 1;
				if (i > 0) {
					result[writeIndex++] = ' ';
				}
			}
		}
		System.out.println(new String(result));
	}
}
