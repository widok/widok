package org.widok

/**
 * This file defines functions to be implemented by streaming data structures, i.e.
 * by children of channels and aggregates.
 *
 * If an operation produces a single value, the return type is ReadChannel[_],
 * otherwise Out[_].
 */

/** Unbounded stream. */
trait StreamFunctions[Out[_], T] {
  def take(count: Int): Out[T]
  def skip(count: Int): Out[T]

  /**
   * Returns first element
   *
   * @note Channels: First produced element after head() was called; it is not the
   *       first value after an observer is attached to head()'s result.
   * @note Aggregates: Produce a value for the first row. If the list is empty,
   *       head does not produce any value. If the first row is deleted, the next
   *       row will be produced.
   */
  def head: ReadChannel[T]

  def isHead(element: T): ReadChannel[Boolean]
  def tail: Out[T]
}

trait BoundedStreamFunctions[Out[_], T] extends StreamFunctions[Out, T] {
  /**
   * Returns first element with the possibility of non-existence
   *
   * @note Channels: TBD
   * @note Aggregates: Some(First row) or None if the list is empty.
   */
  def headOption: ReadChannel[Option[T]]

  def lastOption: ReadChannel[Option[T]]
  def last: ReadChannel[T]
  def isLast(element: T): ReadChannel[Boolean]
  def splitAt(element: T): (Out[T], Out[T])
}

trait FilterFunctions[Out[_], T] {
  def filter(f: T => Boolean): Out[T]
  def filterCh(f: ReadChannel[T => Boolean]): Out[T]
  def distinct: Out[T]
  def span(f: T => Boolean): (Out[T], Out[T])
  def partition(f: T => Boolean): (Out[T], Out[T])
}

trait FoldFunctions[T] {
  def foldLeft[U](acc: U)(f: (U, T) => U): ReadChannel[U]
  def exists(f: T => Boolean): ReadChannel[Boolean]
  def forall(f: T => Boolean): ReadChannel[Boolean]

  def sum[U >: T](implicit num: Numeric[U]): ReadChannel[U] =
    foldLeft(num.zero)(num.plus)
}

trait MapFunctions[Out[_], T] {
  def map[U](f: T => U): Out[U]
  def partialMap[U](f: PartialFunction[T, U]): Out[U]
  def flatMap[U](f: T => Out[U]): Out[U]
  def takeUntil(ch: Channel[_]): Out[T]

  /**
   * Equality check
   *
   * @note Channels: The current value matches.
   * @note Aggregates: All elements have the same value.
   */
  def equal(value: T): ReadChannel[Boolean]
}

trait IterateFunctions[T] {
  def foreach(f: T => Unit): ReadChannel[Unit]

  /**
   * Stream contains at least one occurrence of ``value``.
   *
   * @note Channels: Once true, will never produce any other value.
   * @note Aggregates: When the item is removed, it will produce false.
   */
  def contains(value: T): ReadChannel[Boolean]
}

trait OrderFunctions[T] {
  def get(value: Int): T
  def before(value: T): Option[T]
  def after(value: T): Option[T]
  def indexOf(value: T): Int
  def toSeq: Seq[T]

  def apply(value: Int): T = get(value)
}

trait UpdateFunctions[T] {
  def update(f: T => T)
  def clear()
}

trait UpdateSequenceFunctions[Container[_], T] {
  def prepend(elem: T)
  def append(elem: T)
  def insertBefore(ref: T, elem: T)
  def insertAfter(ref: T, elem: T)
  def remove(handle: T)
  def appendAll(agg: Container[T])
  def removeAll(agg: Container[T])
  def set(items: T*)
  def +=(value: T) = append(value)
  def ++=(agg: Container[T]) = appendAll(agg)
  def -=(value: T) = remove(value)
  def --=(agg: Container[T]) = removeAll(agg)
}

trait SizeFunctions[T] {
  def size: ReadChannel[Int]
  def isEmpty: ReadChannel[Boolean]
  def nonEmpty: ReadChannel[Boolean]
}
