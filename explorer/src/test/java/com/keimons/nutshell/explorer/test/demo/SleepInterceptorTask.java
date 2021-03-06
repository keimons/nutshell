package com.keimons.nutshell.explorer.test.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 采用休眠策略带有拦截器的任务
 * <p>
 * 任务只能执行一次，所以，由参与的线程竞争执行这个执行权限，
 * 竞争成功的线程执行任务，竞争失败的线程直接休眠。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
public class SleepInterceptorTask implements Runnable {
	final Runnable task; // 等待指定的任务
	int counter; // 参与这个任务的线程数量
	Lock lock = new ReentrantLock(); // 这是一个锁
	List<Thread> waits = new ArrayList<>(); // 睡觉的线程

	/**
	 * 构造方法 构造一个拦截器
	 *
	 * @param task    等待执行的任务
	 * @param nThread 有多少个线程共享这个任务
	 */
	public SleepInterceptorTask(Runnable task, int nThread) {
		this.task = task;
		this.counter = nThread;
	}

	@Override
	public void run() {
		boolean isRun = false;
		lock.lock(); // 为了截屏能截完整，去掉了try - finally
		if (counter <= 1) {
			// 终于，终于，只剩下一个线程了，没错，就是你干活！
			isRun = true;
		} else {
			counter--; // 太棒了，俺不用干活儿，记个数而已
			waits.add(Thread.currentThread());
		}
		lock.unlock();
		if (isRun) {
			task.run();
			waits.forEach(LockSupport::unpark);
		} else {
			LockSupport.park(); // 睡觉去吧...
		}
	}
}
