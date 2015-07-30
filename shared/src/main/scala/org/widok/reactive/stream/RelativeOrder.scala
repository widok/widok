package org.widok.reactive.stream

import org.widok.{ReadPartialChannel, ReadChannel}

trait RelativeOrder[T] {
  /**
   * @note Buffers: Current row before `value`
   * @note Channels: Value that is produced right before each `value`
   */
  def before(value: T): ReadChannel[T]

  /**
   * @note Buffers: Current row after `value`
   * @note Channels: Value that is produced right after each `value`
   */
  def after(value: T): ReadChannel[T]

  /** @see [[before]] */
  def beforeOption(value: T): ReadChannel[T]

  /** @see [[after]] */
  def afterOption(value: T): ReadChannel[T]
}
