package com.keimons.nutshell.explorer;

import com.keimons.nutshell.explorer.support.Threadripper;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
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
 * @since 17
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
	 * @param task  任务
	 * @param fence 执行屏障
	 * @throws UnsupportedOperationException 部分实现可能不支持此操作
	 */
	default void executeNow(Runnable task, Object fence) {
		throw new UnsupportedOperationException();
	}

	/**
	 * 提交任务
	 * <p>
	 * 该方法可能会阻塞当前线程，直到任务执行完毕。
	 *
	 * @param task  任务
	 * @param fence 执行屏障
	 * @return 待完成任务的异步计算的结果
	 */
	Future<?> submit(Runnable task, Object fence);

	/**
	 * 提交任务（立即执行）
	 * <p>
	 * 将提交的任务置于队首，尽可能的立即执行，但是并不能保证会被立即执行。
	 * 多线程情况下，即便是置于队首的任务，也有可能在执行之前，被其它任务挤占。
	 *
	 * @param task  任务
	 * @param fence 执行屏障
	 * @return 待完成任务的异步计算的结果
	 * @throws UnsupportedOperationException 部分实现可能不支持此操作
	 */
	default Future<?> submitNow(Runnable task, Object fence) {
		throw new UnsupportedOperationException();
	}

	/**
	 * 提交任务
	 *
	 * @param <T>   返回值类型
	 * @param task  任务
	 * @param fence 执行屏障
	 * @return 待完成任务的异步计算的结果
	 */
	<T> Future<T> submit(Callable<T> task, Object fence);

	/**
	 * 提交任务（立即执行）
	 * <p>
	 * 将提交的任务置于队首，尽可能的立即执行，但是并不能保证会被立即执行。
	 * 多线程情况下，即便是置于队首的任务，也有可能在执行之前，被其它任务挤占。
	 *
	 * @param <T>   返回值类型
	 * @param task  任务
	 * @param fence 执行屏障
	 * @return 待完成任务的异步计算的结果
	 * @throws UnsupportedOperationException 部分实现可能不支持此操作
	 */
	default <T> Future<T> submitNow(Callable<T> task, Object fence) {
		throw new UnsupportedOperationException();
	}

	boolean isShutdown();

	/**
	 * 关闭执行器
	 * <p>
	 * 关闭执行器的流程：
	 * <ol>
	 *     <li>停止接收新任务；</li>
	 *     <li>处理正在排队的任务；</li>
	 *     <li>退出工作线程、（如果有）守护线程。</li>
	 * </ol>
	 * 已经添加到队列中的任务都会正常处理完成，这是友好的，尤其是对于{@link Threadripper}，
	 * 因为任务的重排序，使得任务的执行已经是乱序了，只有等待所有任务执行完成后关闭执行器，
	 * 才能保证任务的执行顺序是符合预期的。如果执行器已经关闭，重复调用不会有额外的效果。
	 * <p>
	 * <p>
	 * 注意，此方法执行完成并不代表执行器已经关闭，若要同步等待执行器关闭完成，请参考{@link #close(RunnableFuture)}方法。
	 */
	void close();

	/**
	 * 关闭执行器并执行给定的任务
	 * <p>
	 * 关闭执行器的同时，允许添加一个{@link RunnableFuture}任务，当执行器成功关闭后，会调用这个任务。
	 * 给定任务的执行有两种可能：
	 * <dl>
	 *     <dt>同步执行</dt>
	 *     <dd>执行器已关闭（守护线程已退出）时，执行器中所有线程已经销毁，没有线程能执行这个任务，
	 *     所以将在调用者本地线程直接执行这个任务。</dd>
	 *     <dt>异步执行</dt>
	 *     <dd>执行器未关闭（守护线程未退出）时，将任务添加到等待队列中，等待执行器关闭后执行。</dd>
	 * </dl>
	 * 重复/多次调用，除了添加多个任务，不会有额外的效果。所有添加的任务都会在执行器关闭后执行，不能保证执行顺序。
	 * 调用者可以同步等待执行器关闭：
	 * <pre>{@code
	 *     ReorderedExplorer explorer = new ReorderedExplorer(4);
	 *     explorer.execute(task, key);
	 *     FutureTask<?> onClose = new FutureTask<>(() -> System.out.println("done."), null);
	 *     explorer.close(onClose);
	 *     onClose.get(); // wait for close
	 * }</pre>
	 * <p>
	 * <p>
	 * 注意：方法的返回不代表执行器已经关闭，但是可以借助{@link RunnableFuture}来实现。
	 *
	 * @param onClose 执行器成功关闭后执行的给定任务
	 */
	void close(@Nullable RunnableFuture<?> onClose);

	/**
	 * 关闭执行器并执行消费者
	 * <p>
	 * 关闭执行器的流程：
	 * <ol>
	 *     <li>停止接收新任务；</li>
	 *     <li>停止处理正在排队的任务；</li>
	 *     <li>退出工作线程、（如果有）守护线程。</li>
	 * </ol>
	 * 除了尽最大努力停止处理正在执行的任务之外，没有任何保证。内部通过{@link Thread#interrupt()}中断任务，
	 * 任何未能响应中断的任务都可能无法终止（甚至永远不会终止）。
	 * 代码示例：
	 * <p>
	 * <p>
	 * 注意：传递给消费者的任务顺序并不保证是添加时的顺序，任务有可能重排序。正在执行中的任务，不论是否执行完成，
	 * 都不会被被添加到消费队列中。
	 *
	 * @param onClose 执行器成功关闭后执行的给定任务
	 */
	void shutdown(@Nullable ConsumerFuture<List<Runnable>> onClose);
}
