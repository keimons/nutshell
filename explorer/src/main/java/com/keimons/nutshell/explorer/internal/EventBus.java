package com.keimons.nutshell.explorer.internal;

import com.keimons.nutshell.core.RunnableInterceptor;
import org.jetbrains.annotations.Nullable;

/**
 * 事件总线
 * <p>
 * 事件总线的设计保证了在不添加派发线程的情况下，又能处理交叉投递问题。
 * <p>
 * 这是一个定制化的事件总线，事件总线维护一个消息队列和一个{@link #writerIndex()}写入位置，
 * 但是，并不维护读取位置，读取位置由读取者自行维护，事件总线而是查找下一个写入位置是否可用，
 * 如果下一个写入位置可用，则直接在该位置写入事件，如果不可用，则调用等待策略。
 * <p>
 * 事件总线的使用：
 * <ol>
 *     <li>发布事件到事件总线；</li>
 *     <li>消费线程读取事件总线中的事件；</li>
 *     <li>在事件总线中移除事件。</li>
 * </ol>
 * 事件总线设计可以是多种多样的，例如：
 * <ul>
 *     <li>发布订阅，事件总线可以采用发布订阅模式，在事件发布时将其投递给对应的消费者。</li>
 *     <li>消息队列，将事件发布在消费队列，等待消费线程读取队列中的内容。</li>
 * </ul>
 * 事件总线也许只是一个抽象的概念，它也许并不是真实存在的，我们仅仅是希望能够有一个“中心”的地方，
 * 将所有的事件存放在中心，它没有FIFO之类的概念，需要的“人”在事件总线读取和销毁事件。
 * 它的设计就像是在漫步，遇到不同的风景，走走停停。
 * <p>
 * <p>
 * Explorer没有采用发布订阅的模式，为其保留了夸进程事件总线的可能性。
 * <p>
 * <p>
 * <p>
 * {@code IO线程 -> 派发线程 -> work线程}的模式能够避免任务的交叉投递，但是增加了一次额外的派发。
 * 取消{@code 派发线程}，则有可能产生交叉投递问题。按照顺序投递（KeyC + KeyA也按照先投递KeyA，再投递KeyC），也可能产生的交叉投递问题如下：
 * <pre>
 * IO-Thread-1, commit task1: KeyA + KeyB
 * IO-Thread-2, commit task2: KeyB + KeyC
 * IO-Thread-3, commit task3: KeyA + KeyC
 * </pre>
 * KeyA，KeyB，KeyC分别投递到队列：QueueA，QueueB，QueueC。在某一个时刻，任务投递情况如下：
 * <ul>
 *     <li>IO-Thread-3，投递task3到QueueA</li>
 *     <li>IO-Thread-1，投递task1到QueueA</li>
 *     <li>IO-Thread-1，投递task1到QueueB</li>
 *     <li>IO-Thread-2，投递task2到QueueB</li>
 *     <li>IO-Thread-2，投递task2到QueueC</li>
 *     <li>IO-Thread-3，投递task3到QueueC</li>
 * </ul>
 * 此时，各个队列中的任务：
 * <pre>
 * QueueA --> task1, task3 --> Thread-1
 * QueueB --> task2, task1 --> Thread-2
 * QueueC --> task3, task2 --> Thread-3
 * </pre>
 * 此时任务出现交叉，产生死锁。在不添加派发线程的前提下，升级环形队列，增加一个维度，每个线程仅仅读取指定槽位的Key，如果当前位置为空，则表示没有任务，跳过执行。示意如下：
 * <pre>
 *           writeIndex
 *           |
 *           +---------------------------+    +---------------------+
 * QueueA -> | Key1 | Key1 |      | Key1 | -> | Thread-1, readIndex |
 *           |------+------+------+------|    |---------------------|
 * QueueB -> |      | Key2 | Key2 |      | -> | Thread-2, readIndex |
 *           |------+------+------+------|    |---------------------|
 * QueueC -> | Key3 |      | Key3 |      | -> | Thread-3, readIndex |
 *           +---------------------------+    +---------------------+
 *              |      |      |      |
 *            task3  task1  task2  task0
 * </pre>
 * 由IO线程生成任务信息并发布在环形Buffer总线上。环形buffer中发布的，不再是单个任务，而是包含Key组的任务，Key组中可能包含一个或多个Key。
 * 仅仅维护一个全局的{@code writeIndex}，每个线程维护自己的{@code readIndex}，只要{@code readIndex < writeIndex}
 * 则可以继续向下读取，如果当前位置为空，则表明此任务不是这个线程关注的任务，跳过执行，联合{@link RunnableInterceptor}使用。
 * <p>
 * 轨道缓冲区同时也是总线队列，所有任务都是发布在总线上。
 * <p>
 * 其它：
 * <ul>
 *     <li>任务发布，环形Buffer总线上发布由IO线程生成的任务。</li>
 *     <li>占用更多空间（n * ThreadCount），队列利用率下降。</li>
 *     <li>任务命中率降至{@code 1/ThreadCount}。</li>
 *     <li>充分利用cpu缓存行能力下降。</li>
 * </ul>
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 **/
public interface EventBus<T> {

	/**
	 * 获取当前写入位置
	 * <p>
	 * 每个消费线程都会自行维护一个读取位置，当读取位置早于写入位置时，才能在事件总线中读出正确的事件。
	 * 遗憾的是，我们总是需要一个“锁”，来保证写入位置的可行性。
	 *
	 * @return 当前写入位置
	 */
	long writerIndex();

	/**
	 * 发布一个事件
	 *
	 * @param event 事件
	 * @return {@code true}发布成功，{@code false}发布失败
	 */
	boolean publishEvent(T event);

	/**
	 * 返回事件序列对应的事件
	 * <p>
	 * 根据事件序列在事件总线中查找并返回事件。
	 *
	 * @param sequence 事件序列
	 * @return 事件序列对应的事件，如果事件已经被移除，则有可能为空。
	 */
	@Nullable T getEvent(long sequence);

	/**
	 * 移除事件
	 * <p>
	 * 注意，不是完成，仅仅代表事件被消费者线程消耗了，并不代表事件已经被完成。任何一个事件，
	 * 最终只会被一个线程消费，同时也意味着由该线程调用此方法移除事件总线中的这个事件。
	 * <p>
	 * 事件的移除并不代表事件完成，有可能是该线程判断事件由该线程完成，将事件缓存到线程本地。
	 *
	 * @param sequence 事件序列
	 */
	void removeEvent(long sequence);

	/**
	 * 返回该读取位置是否已达队尾
	 * <p>
	 * 当事件总线关闭后，此方法才真正有效，否则此方法应该返回{@code false}。
	 * 当关闭事件总线后，并且已经读到队尾，则代表该线程在事件总线上已经没有任何可以读取的事件了。
	 *
	 * @param readerIndex 读取位置
	 * @return {@code true}已达队尾，{@code false}未达队尾
	 */
	boolean eof(long readerIndex);

	/**
	 * 关闭事件总线
	 * <p>
	 * 事件总线的关闭并不会移除任何事件，仅仅是将事件总线的状态修改为不可写入状态。
	 * 事件总线中的事件，只能由消费者进行消费。
	 */
	void shutdown();
}
