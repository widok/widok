package org.widok.reactive.stream

import org.widok.{ReadPartialChannel, ReadChannel}

/** These operations obtain rows depending on absolute positions in a list */
trait AbsoluteOrder[Container[_], T] {
  /**
   * Wraps first element in a channel
   *
   * Produce a value for the first row. If the list is empty, [[head]] does not
   * produce any value. When the first row is deleted, the next row will be
   * produced.
   */
  def head: ReadChannel[T]

  /**
   * First element with the possibility of non-existence
   *
   * @return [[scala.Some]] with first row, or [[scala.None]] if the list is
   *         empty
   */
  def headOption: ReadPartialChannel[T]

  /**
   * Last element with the possibility of non-existence
   *
   * @return [[scala.Some]] with first row, or [[scala.None]] if the list is
   *         empty
   */
  def lastOption: ReadPartialChannel[T]

  /* All rows but first */
  def tail: Container[T]

  /* Last element */
  def last: ReadChannel[T]

  /* Checks whether the given element is the first row */
  def isHead(element: T): ReadChannel[Boolean]

  /* Checks whether the given element is the last row */
  def isLast(element: T): ReadChannel[Boolean]

  /* Splits buffer into two sub-buffers at the given element */
  def splitAt(element: T): (Container[T], Container[T])
}
