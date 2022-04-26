package com.keimons.nutshell.core.command;

/**
 * 命令提取器
 * <p>
 * 从消息中提取消息号和消息体。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public interface CommandExtractor<IN_BOUND, OUT_BOUND, COMMAND_TYPE, MESSAGE_TYPE> {

	/**
	 * 获取命令号
	 *
	 * @param inbound 入栈消息类型
	 * @return 命令号
	 */
	COMMAND_TYPE getCommand(IN_BOUND inbound);

	/**
	 * 获取消息体
	 *
	 * @param inbound 入栈消息类型
	 * @return 消息体
	 */
	MESSAGE_TYPE getMessage(IN_BOUND inbound);

	/**
	 * 生成一个消息体
	 *
	 * @param command 命令号
	 * @param message 消息体
	 * @return 消息体
	 */
	OUT_BOUND buildMessage(COMMAND_TYPE command, MESSAGE_TYPE message);
}
