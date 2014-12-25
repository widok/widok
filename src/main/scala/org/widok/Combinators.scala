package org.widok

/**
 * This file defines functions to be implemented by streaming data structures, i.e.
 * by children of channels and aggregates.
 *
 * If an operation produces a single value, the return type is ReadChannel[_],
 * otherwise Out[_].
 *
 * Var, LazyVar and Opt are StateChannels.
 * VarBuf and Buffer are Aggregates.
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
   */
  def head: ReadChannel[T]

  def isHead(element: T): ReadChannel[Boolean]
  def tail: Out[T]
}

trait BoundedStreamFunctions[Out[_], T] {
  /**
   * Returns first element
   *
   * @note Aggregates: Produce a value for the first row. If the list is empty,
   *       head does not produce any value. If the first row is deleted, the next
   *       row will be produced.
   */
  def head: ReadChannel[Ref[T]]
  /**
   * Returns first element with the possibility of non-existence
   *
   * @note Aggregates: Some(First row) or None if the list is empty.
   */
  def headOption: ReadChannel[Option[Ref[T]]]
  def lastOption: ReadChannel[Option[Ref[T]]]
  def tail: Out[T]
  def last: ReadChannel[Ref[T]]
  def isHead(element: Ref[T]): ReadChannel[Boolean]
  def isLast(element: Ref[T]): ReadChannel[Boolean]
  def splitAt(element: Ref[T]): (Out[T], Out[T])
}

trait FilterFunctions[Out[_], T] {
  def filter(f: T => Boolean): Out[T]
  def distinct: Out[T]
  def span(f: T => Boolean): (Out[T], Out[T])
  def partition(f: T => Boolean): (Out[T], Out[T])
}

trait FoldFunctions[T] {
  def find(f: T => Boolean): ReadChannel[Option[T]]
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
  def takeUntil(ch: ReadChannel[_]): Out[T]

  /**
   * Equality check
   *
   * @note Channels: The current value matches.
   * @note Aggregates: All elements have the same value.
   */
  def equal(value: T): ReadChannel[Boolean]

  /**
   * Inequality check
   *
   * @see equal()
   */
  def unequal(value: T): ReadChannel[Boolean]
}

trait SequentialFunctions[T] {
  /* If sequential access is needed, the underlying functions Seq provides
   * may be accessed.
   */
  def get: Seq[Ref[T]]
  def values: Seq[T]
  def foreach(f: T => Unit)
}

trait IterateFunctions[T] {
  /**
   * Stream contains at least one occurrence of ``value``.
   *
   * @note Channels: Once true, will never produce any other value.
   * @note Aggregates: When the item is removed, it will produce false.
   */
  def contains(value: T): ReadChannel[Boolean]
}

trait UnorderedFunctions[A, B] {
  def value(key: Ref[A]): B
  def apply(key: Ref[A]): B = value(key)
  def update(key: Ref[A], value: B)
  def remove(key: Ref[A])
  def -=(key: Ref[A]) = remove(key)
}

trait OrderFunctions[T] {
  type Index = Int

  def before(handle: Ref[T]): Ref[T]
  def after(handle: Ref[T]): Ref[T]
  def beforeOption(handle: Ref[T]): Option[Ref[T]]
  def afterOption(handle: Ref[T]): Option[Ref[T]]
  def indexOf(handle: Ref[T]): Index
  def toSeq: ReadChannel[Seq[T]]

  def value(index: Index): T
  def apply(index: Index): T = value(index)
}

trait UpdateFunctions[T] {
  def update(f: T => T)
  def clear()
}

trait UpdateSequenceFunctions[Container[_], T] {
  def prepend(element: T): Ref[T]
  def append(element: T): Ref[T]
  def insertBefore(handle: Ref[T], element: T): Ref[T]
  def insertAfter(handle: Ref[T], element: T): Ref[T]
  def remove(handle: Ref[T])
  def appendAll(aggregate: Container[T])
  def removeAll(aggregate: Container[T])
  def set(elements: Seq[Ref[T]])
  def +=(value: T) = append(value)
  def ++=(aggregate: Container[T]) = appendAll(aggregate)
  def -=(value: Ref[T]) = remove(value)
  def --=(agg: Container[T]) = removeAll(agg)
}

trait SizeFunctions {
  /**
   * @note Channels: The size is only produced in response to each
   *       received value on the channel.
   * @note StateChannels: Produce when a new child is attached and if
   *       the size changes. In Opt the size is reset if the value is
   *       cleared.
   * @note Aggregates: Produce the row count once a row is added or
   *       removed.
   */
  def size: ReadChannel[Int]

  /**
   * @note Channels: Produces false with the first received value. Opt
   *       on the other hand will produce true if the current value is
   *       cleared.
   * @note Aggregates: Produce a new value once a row is added or removed.
   */
  def isEmpty: ReadChannel[Boolean]

  /**
   * Negation of ``isEmpty``
   */
  def nonEmpty: ReadChannel[Boolean]
}
