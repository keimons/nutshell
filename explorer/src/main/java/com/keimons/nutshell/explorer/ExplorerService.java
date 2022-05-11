package com.keimons.nutshell.explorer;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 探索者任务执行器
 * <p>
 * 执行提交的带有轨道屏障的{@link Runnable}或{@link Callable}任务的对象。
 * 默认有3个实现，分别是即时执行器、共享队列执行器、轨道队列执行器。
 * 轨道任务执行器并不严格要求执行是异步的。在最简单的情况下，执行者可以立即在调用者的线程中运行提交的任务：
 * <pre>
 *     class DirectExecutor implements TrackExecutor {
 *
 *         void execute(TrackBarrier barrier, Runnable task) {
 *             task.run();
 *         }
 *     }
 * </pre>
 * 轨道队列执行器允许每个线程拥有自己的任务队列，在运行时根据{@code hash}值决定投递到哪个队列。
 * <pre>
 *     class QueueTrackExecutor implements TrackExecutor {
 *
 *         Worker[] workers = new Worker[16];
 *
 *         void execute(TrackBarrier barrier, Runnable task) {
 *             workers[hash % 16].queue.offer(task);
 *         }
 *
 *         static class Worker implements Runnable {
 *
 *             Queue queue = new Queue();
 *
 *             Override
 *             public void run() {
 *                 while (running) {
 *                     Runnable runnable = queue.take();
 * 					   runnable.run();
 *                 }
 *             }
 *         }
 *     }
 * </pre>
 * 同样，轨道任务执行器也支持共享任务队列的线程池，它是{@link ThreadPoolExecutor}的拓展，
 * 当且仅当当前任务执行完成时，才会在任务队列中取出任务并执行。
 * <p>
 * 哈希任务执行器支持任务插入队首，{@code **Now}但并不能保证会被立即执行，在多线程环境中，
 * 即便任务是当前处于队首，但依然有可能在执行之前，被其它线程插入其它任务。
 * <p>
 * 即时执行器和共享队列执行器可能不需要使用哈希值，该值有可能被忽略。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public interface ExplorerService {

	/**
	 * 获取任务执行器的名称
	 *
	 * @return 任务执行器的名称
	 */
	String getName();

	/**
	 * 获取线程池大小
	 *
	 * @return 线程池大小
	 */
	int size();

	/**
	 * 提交任务
	 * <p>
	 * 依赖于不同的实现，该任务可能在新线程池或线程中执行，也可能直接使用调用者的线程立即执行。
	 *
	 * @param task  任务
	 * @param fence 执行屏障
	 */
	void execute(Runnable task, Object fence);

	/**
	 * 提交任务（立即执行）
	 * <p>
	 * 将提交的任务置于队首，尽可能的立即执行，但是并不能保证会被立即执行。
	 * 多线程情况下，即便是置于队首的任务，也有可能在执行之前，被其它任务挤占。
	 *
	 * @param task    任务
	 * @param barrier 执行屏障
	 * @throws UnsupportedOperationException 部分实现可能不支持此操作
	 */
	default void executeNow(Runnable task, TrackBarrier barrier) {
		throw new UnsupportedOperationException();
	}

	/**
	 * 提交任务
	 * <p>
	 * 该方法可能会阻塞当前线程，直到任务执行完毕。
	 *
	 * @param task    任务
	 * @param barrier 执行屏障
	 * @return 待完成任务的异步计算的结果
	 */
	Future<?> submit(Runnable task, TrackBarrier barrier);

	/**
	 * 提交任务（立即执行）
	 * <p>
	 * 将提交的任务置于队首，尽可能的立即执行，但是并不能保证会被立即执行。
	 * 多线程情况下，即便是置于队首的任务，也有可能在执行之前，被其它任务挤占。
	 *
	 * @param task    任务
	 * @param barrier 执行屏障
	 * @return 待完成任务的异步计算的结果
	 * @throws UnsupportedOperationException 部分实现可能不支持此操作
	 */
	default Future<?> submitNow(Runnable task, TrackBarrier barrier) {
		throw new UnsupportedOperationException();
	}

	/**
	 * 提交任务
	 *
	 * @param <T>     返回值类型
	 * @param task    任务
	 * @param barrier 执行屏障
	 * @return 待完成任务的异步计算的结果
	 */
	<T> Future<T> submit(Callable<T> task, TrackBarrier barrier);

	/**
	 * 提交任务（立即执行）
	 * <p>
	 * 将提交的任务置于队首，尽可能的立即执行，但是并不能保证会被立即执行。
	 * 多线程情况下，即便是置于队首的任务，也有可能在执行之前，被其它任务挤占。
	 *
	 * @param <T>     返回值类型
	 * @param task    任务
	 * @param barrier 执行屏障
	 * @return 待完成任务的异步计算的结果
	 * @throws UnsupportedOperationException 部分实现可能不支持此操作
	 */
	default <T> Future<T> submitNow(Callable<T> task, TrackBarrier barrier) {
		throw new UnsupportedOperationException();
	}

	boolean isShutdown();

	/**
	 * 关闭线程池
	 */
	void shutdown();
}