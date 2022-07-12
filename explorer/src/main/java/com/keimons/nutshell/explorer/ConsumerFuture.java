package com.keimons.nutshell.explorer;

import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * 将在未来的某一刻被消费
 *
 * @param <V> 返回值类型
 */
public interface ConsumerFuture<V> extends Consumer<V>, Future<V> {

}
