package org.widok.reactive.stream

import org.widok.ReadChannel

trait Empty {
  /**
   * @note Buffers: Produce a new value once a row is added or removed.
   * @note Channels: Produce false with the first received value.
   * @note Partial channels: Produce true if the current value is cleared.
   */
  def isEmpty: ReadChannel[Boolean]

  /**
   * Negation of [[isEmpty]]
   */
  def nonEmpty: ReadChannel[Boolean]
}
