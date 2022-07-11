package com.keimons.nutshell.explorer.test.performance;

import com.keimons.deepjson.util.UnsafeUtil;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;

/**
 * 数组设置值性能测试
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
public class ArraySetPerformanceTest {

	private static final VarHandle AA = MethodHandles.arrayElementVarHandle(Object[].class);

	@Test
	public void test() {
		Object[] values0 = new Object[1_000_000];
		Object[] values1 = new Object[1_000_000 + 4];
		Object[] values2 = new Object[1_000_000];
		List<Object> objs = new ArrayList<>(1_000_000);
		for (int i = 0; i < 1_000_000; i++) {
			objs.add(new Object());
		}
		Unsafe unsafe = UnsafeUtil.getUnsafe();
		long scale = Unsafe.ARRAY_OBJECT_INDEX_SCALE;
		long offset = Unsafe.ARRAY_OBJECT_BASE_OFFSET + scale * 4;
		System.gc();
		System.gc();
		System.gc();
		System.gc();
		System.gc();
		System.gc();
		long startTime0 = System.nanoTime();
		for (int i = 0; i < 1_000_000; i++) {
			for (int j = 0; j < 1000; j++) {
				if ((j & 1) == 0) {
					AA.set(values0, i, objs.get(999_999 - i));
				} else {
					AA.set(values0, i, objs.get(i));
				}
			}
		}
		long finishTime0 = System.nanoTime();
		long startTime1 = System.nanoTime();
		for (int i = 0; i < 1_000_000; i++) {
			for (int j = 0; j < 1000; j++) {
				if ((j & 1) == 0) {
					unsafe.putObject(values1, offset + scale * i, objs.get(999_999 - i));
				} else {
					unsafe.putObject(values1, offset + scale * i, objs.get(i));
				}
			}
		}
		long finishTime1 = System.nanoTime();
		long startTime2 = System.nanoTime();
		for (int i = 0; i < 1_000_000; i++) {
			for (int j = 0; j < 1000; j++) {
				if ((j & 1) == 0) {
					values2[i] = objs.get(999_999 - i);
				} else {
					values2[i] = objs.get(i);
				}
			}
		}
		long finishTime2 = System.nanoTime();
		System.out.println("time0: " + (finishTime0 - startTime0) + ", time1: " + (finishTime1 - startTime1) + ", time2: " + (finishTime2 - startTime2) + ", total: " + ((finishTime1 - startTime1) * 1f / (finishTime0 - startTime0) * 100) + "%");
	}
}
