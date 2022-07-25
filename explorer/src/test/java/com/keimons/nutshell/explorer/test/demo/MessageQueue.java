package com.keimons.nutshell.explorer.test.demo;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

/**
 * 消息队列
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 */
public class MessageQueue {

	/**
	 * 模拟固定顺序加锁
	 */
	public void lock() {
		Stream<Point> stream = Stream.of(
				new Point(123, 456),
				new Point(123, 457),
				new Point(124, 456),
				new Point(124, 457)
		);
		Stream<Point> points = stream.sorted(Point::compareTo);
		points.forEach(point -> point.lock.lock());
		// move city
		points.forEach(point -> point.lock.unlock());
	}

	/**
	 * 地图中的坐标点
	 */
	public static class Point implements Comparable<Point> {

		int x, y;
		Lock lock = new ReentrantLock();

		Point(int x, int y) {
			this.x = x;
			this.y = y;
		}

		@Override
		public int compareTo(@NotNull MessageQueue.Point o) {
			int result = x - o.x;
			return result == 0 ? y - o.y : result;
		}
	}
}
