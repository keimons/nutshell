package com.keimons.nutshell.core.assembly;

import com.keimons.nutshell.core.ApplicationContext;
import com.keimons.nutshell.core.Autolink;
import com.keimons.nutshell.core.Hotswappable;
import com.keimons.nutshell.core.inject.Injectors;
import com.keimons.nutshell.core.internal.utils.ClassUtils;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * AbstractAssembly
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public abstract class AbstractAssembly<T extends Namespace> implements Assembly {

	/**
	 * 名称
	 * <p>
	 * 名称是{@code Assembly}的唯一标识。在整个生存周期中，只有名称是一定不变的，
	 * 依赖名称查找{@code Assembly}。
	 * <p>
	 * 注意：应用程序中禁止出现两个同名的{@code Assembly}。
	 */
	protected final String name;

	/**
	 * 安装的{@code Assembly}
	 */
	protected T namespace;

	/**
	 * 关注事件列表
	 */
	private List<Hotswappable> hotswappables = new ArrayList<Hotswappable>();

	/**
	 * 关注事件列表
	 */
	private Map<EventType, List<Event>> events = new HashMap<EventType, List<Event>>();

	public AbstractAssembly(String name, T namespace) {
		this.name = name;
		this.namespace = namespace;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void addHotswappable(Hotswappable hotswappable) {
		hotswappables.add(hotswappable);
	}

	@Override
	public List<Hotswappable> getHotswappables() {
		return hotswappables;
	}

	public Map<String, Class<?>> getClasses() {
		return namespace.getClasses();
	}

	public Set<Class<?>> findInjections(Class<? extends Annotation> annotation) {
		return ClassUtils.findInjections(namespace.getClasses().values(), annotation);
	}

	public Object findImplement(String interfaceName) {
		return namespace.getExports().get(interfaceName);
	}

	public void registerImplement(String interfaceName, Object instance) {
		namespace.getExports().put(interfaceName, instance);
	}

	public void inject(ApplicationContext context) throws Throwable {
		Map<String, Object> exports = namespace.getExports();
		for (Object export : exports.values()) {
			if (ClassUtils.hasAnnotation(export.getClass(), Autolink.class)) {
				Injectors injectors = Injectors.of(export.getClass(), Autolink.class);
				injectors.inject(context, this, export);
				System.out.println("inject instance: " + export.getClass());
			}
		}
	}

	@Override
	public void registerEvent(EventType type, Event event) {
		List<Event> events = this.events.computeIfAbsent(type, v -> new ArrayList<Event>());
		events.add(event);
	}

	@Override
	public void onEvent(EventType type, Object... params) throws Throwable {
		List<Event> events = this.events.get(type);
		if (events != null) {
			for (Event event : events) {
				event.onEvent(params);
			}
		}
	}

	@Override
	public String toString() {
		return "assembly: name = '" + name + "'";
	}
}
