package com.keimons.nutshell.explorer.test.demo;

/**
 * 玩家任务执行器
 * <p>
 * 无锁化设计。被投递到执行器的任务，会按照{@link Player#getPlayerId()}进行任务派发。
 * 保证了来自同一个玩家的任务，总是由同一个线程处理，从而防止玩家自身并发问题。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
public class PlayerExecutor {

    private static final int DEFAULT_N_THREAD = 20;

    private static final Worker[] WORKERS = new Worker[DEFAULT_N_THREAD];

    static {
        // 略 init(WORKERS);
    }

    public void execute(Runnable task, Player player) {
        int index = (int) (player.getPlayerId() % DEFAULT_N_THREAD);
        WORKERS[index].offer(task);
    }
}
