package com.keimons.nutshell.explorer.test.demo;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 任务执行者
 * <p>
 * 任务执行者采用多生产者-单消费者模式。每个任务执行者都有一个自己的任务队列，
 * 运行时，不停地在任务队列获取任务，执行任务。任务队列不做任务窃取，
 * 从而保证只要投递到这个队列的任务，必定是由这个线程执行。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
public class Worker {

    private BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();

    public void offer(Runnable task) {
        tasks.offer(task);
    }

    public void startup() {
        while (true) {
            try {
                Runnable take = tasks.take();// 在消息队列中 获取任务、执行任务
                take.run();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
