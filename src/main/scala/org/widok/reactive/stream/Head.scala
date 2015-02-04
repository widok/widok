package org.widok.reactive.stream

import org.widok.ReadChannel

trait Head[T] {
  /**
   * Wraps first element as a channel
   *
   * Denotes first produced element after the [[head]] call; whether [[head]]
   * has observers at this point is irrelevant as the value is cached (i.e.
   * attaching repeatedly will always lead the same value)
   */
  def head: ReadChannel[T]

  /**
   * Checks whether the given element is the first produced value
   */
  def isHead(element: T): ReadChannel[Boolean]
}
