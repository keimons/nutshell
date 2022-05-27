package com.keimons.nutshell.explorer;

import java.util.concurrent.Future;
import java.util.function.Consumer;

public interface ConsumerFuture<V> extends Consumer<V>, Future<V> {

}
