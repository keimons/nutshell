package com.keimons.nutshell.explorer;

import java.util.concurrent.Future;

public interface CompletedFuture<V> extends Future<V> {

	void thenRun(Runnable object);
}
