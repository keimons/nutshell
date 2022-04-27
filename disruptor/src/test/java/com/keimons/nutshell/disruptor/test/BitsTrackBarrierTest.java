package com.keimons.nutshell.disruptor.test;

import com.keimons.nutshell.disruptor.TrackBarrier;
import com.keimons.nutshell.disruptor.internal.BitsTrackBarrier;
import org.junit.jupiter.api.Test;

/**
 * Parkour
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class BitsTrackBarrierTest {

	@Test
	public void test() {
		TrackBarrier barrier0 = new BitsTrackBarrier(8);
		barrier0.init(0, 2, 4, 6);

		TrackBarrier barrier1 = new BitsTrackBarrier(8);
		barrier1.init(0, 1);

		TrackBarrier barrier2 = new BitsTrackBarrier(8);
		barrier2.init(1);

		System.out.println(barrier0.reorder(0, barrier1));
		System.out.println(barrier0.reorder(0, barrier2));
	}
}
