package org.widok.reactive.stream

import org.widok.ReadChannel

trait Take[Container[_], T] {
  /**
   * @note Buffers: Will always contain the first `count` (or less) elements
   *                of the parent buffer.
   * @note Channels: Takes first `count` elements, then stop listening.
   *                 Subscribing to it will always yield the last result as
   *                 initial value.
   */
  def take(count: Int): Container[T]

  /**
   * @note Buffers: Creates a sub-buffer without the first `count` elements.
   * @note Channels: Skips first `count` elements, then stop listening.
   *                 Subscribing to it will always yield the last result as
   *                 initial value.
   */
  def drop(count: Int): Container[T]

  /** Take all elements until `ch` produces any value */
  def takeUntil(ch: ReadChannel[_]): Container[T]
}
