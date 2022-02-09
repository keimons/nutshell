package com.keimons.nutshell.core.internal.utils;

import java.util.function.Consumer;

/**
 * {@link Consumer}工具
 * <p>
 * 通过欺骗编译器实现{@link Consumer}的强制异常检测。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class ThrowableUtils {

	/**
	 * 包装{@link Consumer}以实现必检异常
	 * <p>
	 * 通过方法抛出{@link Throwable}，实现{@link Consumer}创建时的必检异常。
	 * 真实的异常抛出是由{@link ThrowableConsumer}完成的，并通过{@link #juggle(Throwable)}
	 * 欺骗编译器，致使编译器误认为这是一个{@link RuntimeException}。
	 *
	 * @param consumer 消费函数
	 * @param <T>      泛型类型
	 * @return {@link Consumer}带有必检异常的
	 * @throws Throwable 必检异常
	 * @see ThrowableConsumer 抛出异常的{@link Consumer}
	 * @see #juggle(Throwable) 演示如何欺骗编译器
	 */
	public static <T> Consumer<T> wrapper(final ThrowableConsumer<T> consumer) throws Throwable {
		return consumer;
	}

	/**
	 * 包装{@link Runnable}以实现必检异常
	 * <p>
	 * 通过方法抛出{@link Throwable}，实现{@link Runnable}创建时的必检异常。
	 * 真实的异常抛出是由{@link ThrowableRunnable}完成的，并通过{@link #juggle(Throwable)}
	 * 欺骗编译器，致使编译器误认为这是一个{@link RuntimeException}。
	 *
	 * @param runnable 消费函数
	 * @return {@link Runnable}带有必检异常的
	 * @throws Throwable 必检异常
	 * @see ThrowableRunnable 抛出异常的{@link Runnable}
	 * @see #juggle(Throwable) 演示如何欺骗编译器
	 */
	public static Runnable wrapper(final ThrowableRunnable runnable) throws Throwable {
		return runnable;
	}

	/**
	 * 欺骗编译器，让它误以为这是一个{@link RuntimeException}，以允许抛出。
	 */
	@SuppressWarnings("unchecked")
	private static <E extends Throwable> void juggle(final Throwable cause) throws E {
		throw (E) cause;
	}

	@FunctionalInterface
	public interface ThrowableConsumer<T> extends Consumer<T> {

		@Override
		default void accept(final T e) {
			try {
				accept0(e);
			} catch (Throwable cause) {
				juggle(cause);
			}
		}

		void accept0(T e) throws Throwable;
	}

	@FunctionalInterface
	public interface ThrowableRunnable extends Runnable {

		@Override
		default void run() {
			try {
				run0();
			} catch (Throwable cause) {
				juggle(cause);
			}
		}

		void run0() throws Throwable;
	}
}
