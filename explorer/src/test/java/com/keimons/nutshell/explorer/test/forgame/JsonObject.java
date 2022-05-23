package com.keimons.nutshell.explorer.test.forgame;

import java.util.HashMap;
import java.util.Map;

public class JsonObject {

	private final Map<String, Object> json = new HashMap<>();

	public String getString(String key) {
		return (String) json.get(key);
	}
}
