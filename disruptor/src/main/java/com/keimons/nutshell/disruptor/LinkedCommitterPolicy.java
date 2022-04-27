package com.keimons.nutshell.disruptor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 任务排队提交策略
 * <p>
 * 将任务排队提交到任务执行器中，当且仅当当前的任务完成了，才会将下一个任务提交到任务执行器中。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
public class LinkedCommitterPolicy implements CommitterStrategy {

	/**
	 * 任务提交者
	 */
	private final ConcurrentHashMap<Object, Committer> committers = new ConcurrentHashMap<>();
	/**
	 * 过期时间
	 */
	private int overtime;

	public LinkedCommitterPolicy() {
		this(5 * 60 * 1000);
	}

	public LinkedCommitterPolicy(int overtime) {
		this.overtime = overtime;
	}

	@Override
	public void commit(Object key, int executorStrategy, TrackBarrier barrier, Runnable task) {
		Committer committer = committers.computeIfAbsent(key, Committer::new);
		committer.commitTask(executorStrategy, barrier, task);
	}

	@Override
	public void refresh() {
		long currentTime = System.currentTimeMillis();
		for (Object key : committers.keySet()) {
			committers.compute(key, (k, v) -> {
				if (v == null) {
					return null;
				}
				if (currentTime - v.activeTime >= overtime) {
					return null;
				}
				return v;
			});
		}
	}

	public int getOvertime() {
		return overtime;
	}

	public void setOvertime(int overtime) {
		this.overtime = overtime;
	}

	public ConcurrentHashMap<Object, Committer> getCommitters() {
		return committers;
	}

	public static class Committer {

		/**
		 * 空闲中的状态
		 */
		private static final boolean BUSY = true;

		/**
		 * 运行中的状态
		 */
		private static final boolean FREE = false;

		/**
		 * 任务队列的key
		 */
		private final Object key;

		/**
		 * 是否正在执行中
		 */
		private final AtomicBoolean busy = new AtomicBoolean(FREE);

		/**
		 * 等待执行的任务
		 */
		private final ConcurrentLinkedQueue<Work> works = new ConcurrentLinkedQueue<>();

		/**
		 * 上次活跃时间
		 */
		private long activeTime;

		public Committer(Object key) {
			this.key = key;
		}

		public void commitTask(int executorStrategy, TrackBarrier barrier, Runnable task) {
			Work work = new Work(executorStrategy, barrier, task);
			works.offer(work);
			activeTime = System.currentTimeMillis();
			tryStartTask();
		}

		/**
		 * 尝试开始一个任务
		 */
		private void tryStartTask() {
			if (!works.isEmpty() && busy.compareAndSet(FREE, BUSY)) {
				Work work = works.peek();
				if (work == null) {
					busy.set(FREE);
					return;
				}
				TrackExecutor strategy = ExecutorManager.getExecutorStrategy(work.getExecutorStrategy());
				strategy.execute(buildLinkedTask(work), work.getBarrier());
			}
		}

		/**
		 * 完成任务
		 *
		 * @param finish 已经完成的任务
		 */
		public void finishTask(Work finish) {
			works.poll();
			busy.set(FREE);
		}

		/**
		 * 构造一个可以连续执行的任务
		 *
		 * @param work 当前要执行的任务
		 * @return 新的任务
		 */
		private Runnable buildLinkedTask(Work work) {
			return () -> {
				try {
					work.getTask().run();
				} finally {
					finishTask(work);
				}
				tryStartTask();
			};
		}

		public Object getKey() {
			return key;
		}

		public ConcurrentLinkedQueue<Work> getTasks() {
			return works;
		}

		public long getActiveTime() {
			return activeTime;
		}

		public void setActiveTime(long activeTime) {
			this.activeTime = activeTime;
		}
	}

	/**
	 * 等待执行的任务
	 *
	 * @author houyn[monkey@keimons.com]
	 * @version 1.0
	 * @since 11
	 **/
	public static class Work {

		/**
		 * 任务执行策略
		 */
		private int executorStrategy;

		/**
		 * 执行屏障
		 */
		private TrackBarrier barrier;

		/**
		 * 准备执行的任务
		 */
		private Runnable task;

		public Work(int executorStrategy, TrackBarrier barrier, Runnable task) {
			this.executorStrategy = executorStrategy;
			this.barrier = barrier;
			this.task = task;
		}

		public int getExecutorStrategy() {
			return executorStrategy;
		}

		public void setExecutorStrategy(int executorStrategy) {
			this.executorStrategy = executorStrategy;
		}

		public TrackBarrier getBarrier() {
			return barrier;
		}

		public void setBarrier(TrackBarrier barrier) {
			this.barrier = barrier;
		}

		public Runnable getTask() {
			return task;
		}

		public void setTask(Runnable task) {
			this.task = task;
		}
	}
}
