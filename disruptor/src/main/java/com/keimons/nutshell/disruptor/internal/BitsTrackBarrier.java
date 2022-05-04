package com.keimons.nutshell.disruptor.internal;

import com.keimons.nutshell.disruptor.TrackBarrier;
import jdk.internal.vm.annotation.Contended;
import jdk.internal.vm.annotation.ForceInline;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 比特轨道屏障
 * <p>
 * 轨道屏障的一种实现，适用于不超过64线程的线程模型。比特轨道采用bit位运算判断消息是否可以重排序执行。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public class BitsTrackBarrier implements TrackBarrier {

	/**
	 * 轨道数量（最大不超过64）
	 * <p>
	 * 它就像操场上的跑道，共有{@code nTracks}条轨道，每个线程仅仅跑在自己的轨道上。
	 */
	private final int nTracks;

	private final List<Object> fes;

	/**
	 * 轨道中key的数量
	 * <p>
	 * 每个轨道中包含{@code key}的数量。通常情况下，一个轨道中不应该包含过多key。
	 */
	private final int[] tracks;

	/**
	 * 执行栅栏
	 */
	private final Object[][] fences;

	@Contended
	public final AtomicInteger forbids = new AtomicInteger();

	/**
	 * 任务位置
	 * <p>
	 * 共有{@link #nTracks}条轨道，当前任务所处的轨道位置（可能不止一个）。轨道数量不超过64，所以使用{@code bits}存储。
	 */
	private long bits;

	protected volatile boolean intercepted;

	public BitsTrackBarrier(int nTracks) {
		if (nTracks > 64) {
			throw new IllegalArgumentException("nTracks must not more than 64");
		}
		this.fences = new Object[nTracks][4];
		this.tracks = new int[nTracks];
		this.nTracks = nTracks;
		this.fes = new ArrayList<>();
	}

	private void init0(Object fence) {
		int hashcode = fence.hashCode();
		int track = hashcode % nTracks;
		bits |= (1L << track);
		fes.add(fence);

//		Object[] fences = this.fences[track];
//		int index = this.tracks[track];
//		if (index >= fences.length) {
//			Object[] tmp = new Object[index << 1];
//			System.arraycopy(fences, 0, tmp, 0, index);
//			fences = tmp;
//			this.fences[track] = fences;
//		}
//		fences[index] = fence;
//		this.tracks[track] = index + 1;
	}

	@Override
	public void init(Object fence) {
		init0(fence);
		this.intercepted = true;
		this.forbids.set(Long.bitCount(bits) - 1);
	}

	@Override
	public void init(Object fence0, Object fence1) {
		init0(fence0);
		init0(fence1);
		this.intercepted = true;
		this.forbids.set(Long.bitCount(bits) - 1);
	}

	@Override
	public void init(Object fence0, Object fence1, Object fence2) {
		init0(fence0);
		init0(fence1);
		init0(fence2);
		this.intercepted = true;
		this.forbids.set(Long.bitCount(bits) - 1);
	}

	@Override
	public void init(Object... fences) {
		if (fences.length == 0) {
			throw new IllegalArgumentException("no keys");
		}
		for (int i = 0, length = fences.length; i < length; i++) {
			Object key = fences[i];
			init0(key);
		}
		this.intercepted = true;
		this.forbids.set(Long.bitCount(bits) - 1);
	}

	@Override
	public void init(int forbids) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean intercept() {
		throw new UnsupportedOperationException();
	}

	@Override
	@ForceInline
	public boolean tryIntercept() {
		return forbids.getAndDecrement() > 0;
	}

	@Override
	public void setIntercepted(boolean intercepted) {
		this.intercepted = intercepted;
	}

	@Override
	public boolean isIntercepted() {
		return intercepted;
	}

	@Override
	public int[] hashes() {
		int[] result = new int[Long.bitCount(bits)];
		int index = 0;
		for (int i = 0; i < nTracks; i++) {
			if ((bits & (1L << i)) != 0) {
				result[index++] = i;
			}
		}
		return result;
	}

	@Override
	public boolean isTrack(long bits) {
		return (this.bits & bits) != 0;
	}

	@Override
	public boolean reorder(int track, TrackBarrier barrier) {
		if (!(barrier instanceof BitsTrackBarrier)) {
			throw new IllegalStateException("unknown state: " + barrier.getClass());
		}
		BitsTrackBarrier other = (BitsTrackBarrier) barrier;
		if ((bits & other.bits) == 0) {
			throw new IllegalStateException("unknown state: " + barrier.getClass());
		}
		return !fes.contains(track);
//		Object[] keys0 = this.fences[track];
//		Object[] keys1 = other.fences[track];
//		int index0 = this.tracks[track];
//		int index1 = other.tracks[track];
//		for (int i = 0; i < index0; i++) {
//			Object k = keys0[i];
//			for (int j = 0; j < index1; j++) {
//				if (k.equals(keys1[j])) {
//					return false;
//				}
//			}
//		}
//		return true;
	}

	@Override
	public boolean isTrack(int track) {
		return (bits & 1L << track) != 0;
	}

	@Override
	public boolean isSingle(int track) {
		return bits == 1L << track;
	}

	@Override
	public void release() {
//		for (int i = 0; i < tracks.length; i++) {
//			int index = tracks[i];
//			if (index > 0) {
//				tracks[i] = 0;
//				for (int j = 0; j < index; j++) {
//					fences[i][j] = null;
//				}
//			}
//		}
		this.fes.clear();
		this.bits = 0L;
	}
}
