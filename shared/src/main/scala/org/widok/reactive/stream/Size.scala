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
}
