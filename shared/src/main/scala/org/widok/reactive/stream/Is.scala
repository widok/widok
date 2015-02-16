package org.widok.reactive.stream

import org.widok.ReadChannel

/**
 * Operations for streams that can only have one value at the same time, like
 * channels
 */
trait Is[T] {
  /** Current value is equal to `value` */
  def is(value: T): ReadChannel[Boolean]

  /** Current value is not equal to `value` */
  def isNot(value: T): ReadChannel[Boolean]
}
