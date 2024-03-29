package com.keimons.nutshell.explorer.test.forgame;

import java.util.HashMap;
import java.util.Map;

/**
 * 模拟JSON工具
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
public class JsonObject {

	private final Map<String, Object> json = new HashMap<>();

	public JsonObject(Object... kv) {
		for (int i = 0; i < kv.length; i += 2) {
			json.put(kv[i].toString(), kv[i + 1]);
		}
	}

	public String getString(String key) {
		return (String) json.get(key);
	}

	public int getInt(String key) {
		return (Integer) json.get(key);
	}
}
