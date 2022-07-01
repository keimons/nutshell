package com.keimons.nutshell.explorer.test.demo;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 演示版多Hash任务执行器
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
public class DemoMultiHashExecutor extends MultiHashExecutor {

    @Override
    public synchronized void execute(Runnable task, Object... fences) {
        // 统计哪些线程参与这个任务
        Set<Integer> threads = Stream.of(fences)
                .map(fence -> fence.hashCode() % DEFAULT_N_THREAD)
                .collect(Collectors.toSet());
        // 构造一个实现贼垃圾的包装任务
        LJInterceptor wrapperTask = new LJInterceptor(task, threads.size());
        // 把这个任务，丢给多个线程
        threads.forEach(index -> WORKERS[index].offer(wrapperTask));
    }
}
