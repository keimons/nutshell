package com.keimons.nutshell.core;

import com.keimons.nutshell.explorer.support.Threadripper;

/**
 * 撕裂者
 * <p>
 * 核心设计。标注一个类是撕裂者。内部设计有两个撕裂者，分别是：线程撕裂者和进程撕裂者。
 *
 * <dl>
 *     <dt>{@link Threadripper}线程撕裂者</dt>
 *     <dd>采用多线程的线程模型，提供趋近于单线程的最终表现。</dd>
 *     <dt>{@link Progressripper}进程撕裂者</dt>
 *     <dd>采用多进程的进程模型，提供趋近于单进程的最终表现。</dd>
 * </dl>
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 */
public interface Ripper {
}
