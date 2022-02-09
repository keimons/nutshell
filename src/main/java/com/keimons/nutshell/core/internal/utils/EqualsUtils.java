package com.keimons.nutshell.core.internal.utils;

import java.util.Arrays;
import java.util.Map;

public class EqualsUtils {

	public static boolean isEquals(Map<String, byte[]> map1, Map<String, byte[]> map2) {
		if (map1.size() != map2.size()) {
			return false;
		}
		for (Map.Entry<String, byte[]> entry : map1.entrySet()) {
			byte[] bytes2 = map2.get(entry.getKey());
			if (bytes2 == null) {
				return false;
			}
			byte[] bytes1 = entry.getValue();
			if (!Arrays.equals(bytes1, bytes2)) {
				return false;
			}
		}
		return true;
	}
}
