package org.widok.reactive.stream

import org.widok.ReadChannel

trait Size {
  /**
   * @note Buffers: Produce the row count once a row is added or removed.
   * @note Channels: The size is only produced in response to each received
   *                 value on the channel.
   * @note State channels: Produce when a new child is attached and if the size
   *                       changes. In [[org.widok.Opt]] the size is reset if
   *                       the value is cleared.
   */
  def size: ReadChannel[Int]

  /**
   * @note Buffers: Produce a new value once a row is added or removed.
   * @note Channels: Produce false with the first received value.
   * @note Partial channels: Produce true if the current value is cleared.
   */
  def isEmpty: ReadChannel[Boolean] = size.is(0)

  /**
   * Negation of [[isEmpty]]
   */
  def nonEmpty: ReadChannel[Boolean] = size.isNot(0)
}
