package com.keimons.nutshell.core.command;

import com.keimons.nutshell.core.Autolink;
import com.keimons.nutshell.core.internal.utils.MethodUtils;
import com.keimons.nutshell.core.session.Session;
import com.keimons.nutshell.core.session.SessionContext;

import java.lang.invoke.MethodHandle;
import java.util.HashMap;
import java.util.Map;

/**
 * 命令管理器
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
public class ExecutorService {

	Map<Object, MethodHandle> executors = new HashMap<Object, MethodHandle>();

	@Autolink
	private CommandExtractor<Object, Object, Object, Object> extractor;

	@Autolink
	private SessionContext context;

	public void registerCommand(Class<?> clazz) throws Exception {
		Object o = clazz.getConstructor().newInstance();
		Handler handler = clazz.getAnnotation(Handler.class);
		if (handler != null) {
			registerCommand(handler.value(), o);
		}
		Processor processor = clazz.getAnnotation(Processor.class);
		if (processor != null) {
			registerCommand(processor.value(), o);
		}
	}

	/**
	 * 增加一个命令
	 *
	 * @param command  命令
	 * @param executor 命令行动
	 */
	public void registerCommand(Object command, Object executor) {
		if (command instanceof Integer) {
			MethodHandle process = MethodUtils.findMethod(executor.getClass(), "process");
			executors.put(command, process);
		}
		if (command instanceof String) {
			MethodHandle handle = MethodUtils.findMethod(executor.getClass(), "handle");
			executors.put(command, handle);
		}
		throw new RuntimeException();
	}

	/**
	 * 执行一个明明
	 *
	 * @param session 会话
	 * @param inbound 消息
	 * @throws Throwable 执行异常
	 */
	public void execute(Session<?> session, Object inbound) throws Throwable {
		Object command = extractor.getCommand(inbound);
		Object message = extractor.getMessage(inbound);
		MethodHandle executor = executors.get(command);
		executor.invoke(session, message, context);
	}
}
