package com.keimons.nutshell.explorer;

import com.keimons.nutshell.explorer.support.AbortPolicy;

import java.util.concurrent.ThreadFactory;

/**
 * 任务执行器的抽象实现
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public abstract class AbstractExplorerService implements ExplorerService {

	/**
	 * 默认被拒绝任务的处理策略
	 * <p>
	 * 当消息队列已满时，如果继续向队列中写入任务，则调用执行此异常。
	 */
	public static final RejectedExplorerHandler DefaultRejectedHandler = new AbortPolicy();

	/**
	 * 运行中
	 * <p>
	 * 接受新任务并处理排队任务。
	 */
	protected static final int RUNNING = 0;

	/**
	 * 已关闭
	 * <p>
	 * 不接受新任务，正常处理排队中的任务，任务处理完成后线程池关闭。
	 */
	protected static final int CLOSE = 1 << 0;

	/**
	 * 已停止
	 * <p>
	 * 不接受新任务，不处理排队任务，中断正在进行的任务。
	 */
	protected static final int SHUTDOWN = 1 << 1;

	/**
	 * 已终结
	 * <p>
	 * 生命周期的最后一个状态，不接受新任务，不处理排队任务，中断正在进行的任务，尽可能的快速退出。
	 */
	protected static final int TERMINATED = 1 << 2;

	/**
	 * 线程池名称
	 */
	protected final String name;

	/**
	 * 线程数量
	 */
	protected final int nThreads;

	/**
	 * 被拒绝执行的任务处理句柄
	 */
	protected final RejectedExplorerHandler rejectedHandler;

	/**
	 * 是否阻塞调用者线程
	 * <p>
	 * 当队列没有足够空间时，是否阻塞调用者线程。
	 */
	protected final boolean blockingCaller;

	/**
	 * 线程工厂
	 */
	protected final ThreadFactory threadFactory;

	/**
	 * 线程池是否运行中
	 */
	protected volatile boolean running = true;

	/**
	 * 主池控制状态ctl是一个原子整数，
	 * 包装了两个概念字段workerCount，表示有效线程数
	 * runState，表示是否正在运行，关闭等为了将它们打包成一个int，
	 * 我们限制workerCount为(2^29 )-1（约 5 亿）个线程，而不是 (2^31)-1（20 亿）个其他可表示的线程。
	 * 如果这在未来成为问题，可以将变量更改为 AtomicLong，并调整下面的移位/掩码常量。
	 * 但是在需要之前，这段代码使用 int 会更快更简单一些。
	 * workerCount 是允许启动和不允许停止的工作程序的数量。
	 * 该值可能与实际的活动线程数暂时不同，例如
	 * 当 ThreadFactory 在被询问时未能创建线程时，以及退出线程在终止前仍在执行簿记时。
	 * 用户可见的池大小报告为工作集的当前大小。
	 * runState 提供主要的生命周期控制，
	 * 取值： RUNNING：接受新任务并处理排队任务
	 * SHUTDOWN：不接受新任务，但处理排队任务
	 * STOP：不接受新任务，不处理排队任务，并中断正在进行的任务
	 * TIDYING：所有任务都已终止，workerCount 为零，
	 * 转换到状态 TIDYING 的线程将运行 terminate() 钩子方法.
	 * runState 随着时间的推移单调增加，但不需要达到每个状态。
	 * 转换是： RUNNING -> SHUTDOWN 调用 shutdown()
	 * （RUNNING 或 SHUTDOWN） -> STOP 调用 shutdownNow()
	 * SHUTDOWN -> TIDYING 当队列和池都为空时
	 * STOP -> TIDYING 当池为空时
	 * TIDYING -> TERMINATED 当 terminate() 钩子方法完成时，在 awaitTermination() 中等待的线程将在状态达到 TERMINATED 时返回。
	 * 检测从 SHUTDOWN 到 TIDYING 的转换并不像您想要的那么简单，
	 * 因为队列可能在非空后变为空，在 SHUTDOWN 状态下反之亦然，但我们只能在看到它为空之后终止，我们看到 workerCount为 0（有时需要重新检查——见下文）。
	 */
	/**
	 * 线程池状态
	 * <p>
	 * 用于提供生命周期的所有状态。{@code state}的取值有：
	 * <ul>
	 *     <li>{@link #RUNNING}：运行中，接受新任务并处理排队任务。</li>
	 *     <li>{@link #CLOSE}：已关闭，不接受新任务，但处理排队任务。</li>
	 *     <li>{@link #SHUTDOWN}：已停止，不接受新任务，不处理排队任务，中断正在进行的任务。</li>
	 *     <li>{@link #TERMINATED}：已终结，生命周期的最后一个状态。</li>
	 * </ul>
	 */
	protected volatile int state = RUNNING;

	/**
	 * 哈希任务执行器
	 *
	 * @param name            执行器名称
	 * @param nThreads        线程数量
	 * @param rejectedHandler 被拒绝执行任务的处理句柄
	 * @param threadFactory   线程工厂
	 */
	public AbstractExplorerService(String name, int nThreads, RejectedExplorerHandler rejectedHandler, ThreadFactory threadFactory) {
		this.name = name;
		this.nThreads = nThreads;
		this.rejectedHandler = rejectedHandler;
		this.blockingCaller = rejectedHandler instanceof BlockingCallerHandler;
		this.threadFactory = threadFactory;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int size() {
		return nThreads;
	}
}
