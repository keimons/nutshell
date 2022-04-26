package com.keimons.nutshell.core.nutshell;

import com.keimons.nutshell.core.Autolink;
import com.keimons.nutshell.core.sequence.Sequencer;

import java.util.HashMap;
import java.util.Map;

/**
 * 故事的主角
 * <p>
 * 移除常规的用户、玩家和角色的概念，所有数据存放于主角身上。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public abstract class Protagonist {

	protected Map<String, Object> modules = new HashMap<>();

	@Autolink
	private transient Sequencer sequencer;

	private transient Object[] _modules = new Object[16];

	@SuppressWarnings("unchecked")
	public <T> T get(int index) {
		return (T) _modules[index];
	}

	public void putModule(String name, Object module) {
		modules.put(name, module);
		int index = sequencer.getIndex(module.getClass());
		ensureCapacity(index);
		_modules[index] = module;
	}

	private void ensureCapacity(int index) {
		for (; ; ) {
			int length = _modules.length;
			if (index < length) {
				return;
			}
			Object[] newModules = new Object[length << 1];
			System.arraycopy(_modules, 0, newModules, 0, length);
			_modules = newModules;
		}
	}
}
