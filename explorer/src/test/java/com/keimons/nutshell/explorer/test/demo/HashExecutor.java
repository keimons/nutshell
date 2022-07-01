package com.keimons.nutshell.explorer.test.demo;

/**
 * 哈希任务执行器
 * <p>
 * {@link PlayerExecutor}和{@link UnionExecutor}整合版本，能够接受更多种类的业务，
 * 例如：玩家，帮派，组队等等。被投递到任务执行器的任务，
 * 会按照{@link Object#hashCode()}进行任务派发。
 * 派发依据可能是{@link Player#getPlayerId()}、{@link Union#getUnionId()}、
 * {@link Team#getTeamId()}等等，甚至可以是{@link Player}、{@link Union}等。
 * 我们给派发依据，也就是任务提交时附带的{@link Object}，
 * 起了一个响当当的名号：执行屏障，并将其命名为：{@code fence}。
 * 执行屏障（fence）的存在，保证了来自同一个源头的任务，总是由同一个线程执行。
 * <p>
 * 事实上，我们更看重的是它另外的一个特性：
 * <b>带有相同执行屏障（fence）的任务，总是串行执行的</b>。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
public class HashExecutor {

    protected static final int DEFAULT_N_THREAD = 20;

    protected static final Worker[] WORKERS = new Worker[DEFAULT_N_THREAD];

    static {
        // 略 init(WORKERS);
    }

    /**
     * 提交任务
     *
     * @param task  等待执行的任务，将在未来的某个时间点执行。
     * @param fence 执行屏障，可以是{@code playerId}、{@code unionId}等{@code xxxId}，
     *              也可以是{@link Player}、{@link Union}等对象。
     */
    public void execute(Runnable task, Object fence) {
        int index = fence.hashCode() % DEFAULT_N_THREAD;
        WORKERS[index].offer(task);
    }
}
