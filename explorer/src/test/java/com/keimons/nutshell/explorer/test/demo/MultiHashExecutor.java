package com.keimons.nutshell.explorer.test.demo;

/**
 * 执行器：任务带有多个执行屏障
 * <p>
 * 设计目标：
 * <ol>
 *     <li>保持无锁化设计；</li>
 *     <li>任务只能被执行一次；</li>
 *     <li><b>带有多个执行屏障的任务，对于每个执行屏障都是串行的</b>；</li>
 *     <li>尽可能的提升性能。</li>
 * </ol>
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
public class MultiHashExecutor extends HashExecutor {

    @Override
    public void execute(Runnable task, Object fence) {
        super.execute(task, fence);
    }

    public void execute(Runnable task, Object fence0, Object fence1) {
        // ???
    }

    public void execute(Runnable task, Object fence0, Object fence1, Object fence2) {
        // ???
    }

    public void execute(Runnable task, Object... fences) {
        // ???
    }
}
