package com.keimons.nutshell.explorer.support;

import com.keimons.nutshell.explorer.BlockingCallerHandler;
import com.keimons.nutshell.explorer.ExplorerService;
import com.keimons.nutshell.explorer.RejectedTrackExecutionHandler;

import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 共享队列执行器
 * <p>
 * 共享队列执行器是{@link ThreadPoolExecutor}拓展，添加了对于{@link ExplorerService}的支持。
 * <dl>
 *     <dt>固定线程数量</dt>
 *     <dd>共享队列执行器仅支持固定数量的线程池，这也意味着它在运行时更改线程数量是无效的。
 *     不论任务排队与否，都不会创建/销毁线程池。初始化时直接初始化所有线程，而不是等到任务的提交。
 *     <dt>排队</dt>
 *     <dd>共享队列线程池使用{@link LinkedBlockingDeque}，并基于此实现了插队功能。此队列的使用与线程池的交互：
 *     <ul>
 *         <li>{@code execute/commit}总是将任务追加到队尾。</li>
 *         <li>{@code executeNow/commitNow}总是将任务插入到队首。</li>
 *     </ul>
 *     尽管共享队列执行器提供了插队执行，但是并不能保证任务将会被立即执行，因为它可能被其它任务再次插队。
 *     任务排队的策略有以下几种：
 *     <ol>
 *         <li><em>无界队列/有界队列</em>。任务将直接追加到队尾或插入队首。</li>
 *         <li><em>阻塞策略</em>。阻塞提交者线程，直到成功将任务放入队列中。</li>
 *     </ol>
 * </dl>
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class SharedQueueExplorer extends ThreadPoolExecutor implements ExplorerService {

	/**
	 * 执行器名称
	 */
	private final String name;

	/**
	 * 主锁
	 */
	private final ReentrantLock lock = new ReentrantLock();

	/**
	 * 等待条件
	 */
	private final Condition notFull = lock.newCondition();

	/**
	 * 拒绝执行策略
	 * <p>
	 * 当任务无法提交到队列中时调用，例如：
	 * <ul>
	 *     <li>线程队列已满</li>
	 *     <li>执行器已关闭</li>
	 * </ul>
	 */
	private final RejectedTrackExecutionHandler rejectedHandler;

	/**
	 * （阻塞调用者策略）是否阻塞调用者
	 */
	private final boolean blockingCaller;

	/**
	 * 待执行的任务队列
	 */
	private final BlockingDeque<Runnable> sharedQueue;

	/**
	 * 共享队列执行器
	 *
	 * @param name            执行器名称
	 * @param nThreads        启动线程
	 * @param rejectedHandler 拒绝执行策略
	 */
	public SharedQueueExplorer(String name, int nThreads, RejectedTrackExecutionHandler rejectedHandler) {
		this(name, nThreads, new LinkedBlockingDeque<>(8), rejectedHandler);
	}

	private SharedQueueExplorer(String name, int nThreads, BlockingDeque<Runnable> sharedQueue, RejectedTrackExecutionHandler rejectedHandler) {
		super(nThreads, nThreads, 0, TimeUnit.MILLISECONDS, sharedQueue, new InternalRejectedExecutionHandler());
		this.name = name;
		this.sharedQueue = sharedQueue;
		this.rejectedHandler = rejectedHandler;
		this.blockingCaller = rejectedHandler instanceof BlockingCallerHandler;
		init();
	}

	private void init() {
		for (int i = 0; i < getCorePoolSize(); i++) {
			execute(() -> {
			});
		}
	}

	private void reject(Object fence, Runnable task, boolean last) {
		// 检测线程池是否已经关闭，如果线程池关闭，则直接调用拒绝策略
		if (!isShutdown() && blockingCaller) {
			try {
				lock.lockInterruptibly();
				try {
					while (last ? !sharedQueue.offerLast(task) : !sharedQueue.offerFirst(task)) {
						notFull.await();
						// 线程被唤醒后，先检查线程池是否关闭。
						if (isShutdown()) {
							rejectedHandler.rejectedExecution(fence, task, this);
							return;
						}
					}
				} finally {
					lock.unlock();
				}
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		} else {
			rejectedHandler.rejectedExecution(fence, task, this);
		}
	}

	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		super.beforeExecute(t, r);
		if (blockingCaller) {
			try {
				lock.lockInterruptibly();
				try {
					notFull.signal();
				} finally {
					lock.unlock();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int size() {
		return sharedQueue.size();
	}

	@Override
	public void execute(Runnable task, Object fence) {
		try {
			execute(task);
		} catch (InternalException e) {
			reject(fence, task, true);
		}
	}

	@Override
	public void executeNow(Runnable task, Object fence) {
		try {
			execute(task);
		} catch (InternalException e) {
			reject(fence, task, false);
		}
	}

	@Override
	public Future<?> submit(Runnable task, Object fence) {
		if (task == null) {
			throw new NullPointerException();
		}
		RunnableFuture<Void> future = newTaskFor(task, null);
		execute(future, fence);
		return future;
	}

	@Override
	public Future<?> submitNow(Runnable task, Object fence) {
		if (task == null) {
			throw new NullPointerException();
		}
		RunnableFuture<Void> future = newTaskFor(task, null);
		executeNow(future, fence);
		return future;
	}

	@Override
	public <T> Future<T> submit(Callable<T> task, Object fence) {
		if (task == null) {
			throw new NullPointerException();
		}
		RunnableFuture<T> future = newTaskFor(task);
		execute(future, fence);
		return future;
	}

	@Override
	public <T> Future<T> submitNow(Callable<T> task, Object fence) {
		if (task == null) {
			throw new NullPointerException();
		}
		RunnableFuture<T> future = newTaskFor(task);
		executeNow(future, fence);
		return future;
	}

	@Override
	public void shutdown() {
		super.shutdown();
		if (blockingCaller) {
			try {
				lock.lockInterruptibly();
				try {
					notFull.signalAll();
				} finally {
					lock.unlock();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	/**
	 * 关联{@link RejectedExecutionHandler}和{@link RejectedTrackExecutionHandler}
	 */
	private static final class InternalRejectedExecutionHandler implements RejectedExecutionHandler {

		@Override
		public void rejectedExecution(Runnable r, java.util.concurrent.ThreadPoolExecutor executor) {
			throw new InternalException();
		}
	}

	private static final class InternalException extends RuntimeException {

	}
}
