package org.widok

/**
 * This file defines functions to be implemented by streaming data structures,
 * i.e. channels and aggregates. These are called *containers* in the following.
 *
 * If an operation produces a single value, the return type is ReadChannel[_],
 * otherwise Container[_].
 *
 * [[Var]], [[LazyVar]] and [[Opt]] are [[StateChannel]]s.
 * [[Buffer]] and [[OrderedMap]] are [[Aggregate]]s.
 */

trait StreamFunctions[Container[_], T] {
  /**
   * @note Channels: Take first `count` elements, then stop listening.
   * @note Aggregates: Will always contain the first `count` (or less) elements
   *                   of the parent aggregate.
   */
  def take(count: Int): Container[T]

  /**
   * @note Channels: Skip first `count` elements, then stop listening.
   * @note Aggregates: Create a sub-aggregate without the first `count` elements.
   */
  def drop(count: Int): Container[T]
}

/** For channels */
trait UnboundedStreamFunctions[Container[_], T] extends StreamFunctions[Container, T] {
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

  /**
   * Skips first element
   */
  def tail: Container[T]
}

/** For aggregates */
trait BoundedStreamFunctions[Container[_], T] extends StreamFunctions[Container, T] {
  /**
   * Wraps first element in a channel
   *
   * Produce a value for the first row. If the list is empty, [[head]] does not
   * produce any value. When the first row is deleted, the next row will be
   * produced.
   */
  def head: ReadChannel[Ref[T]]

  /**
   * First element with the possibility of non-existence
   *
   * @return [[Some]] with first row, or [[None]] if the list is empty
   */
  def headOption: ReadChannel[Option[Ref[T]]]

  /**
   * Last element with the possibility of non-existence
   *
   * @return [[Some]] with first row, or [[None]] if the list is empty
   */
  def lastOption: ReadChannel[Option[Ref[T]]]

  /**
   * All rows but first
   */
  def tail: Container[T]

  /**
   * Last row
   */
  def last: ReadChannel[Ref[T]]

  /**
   * Checks whether the given element is the first row
   */
  def isHead(element: Ref[T]): ReadChannel[Boolean]

  /**
   * Checks whether the given element is the last row
   */
  def isLast(element: Ref[T]): ReadChannel[Boolean]

  /**
   * Splits aggregate into two sub-aggregates
   */
  def splitAt(element: Ref[T]): (Container[T], Container[T])
}

trait FilterFunctions[Container[_], T] {
  /**
   * Only include elements for which `f` is true
   */
  def filter(f: T => Boolean): Container[T]

  /**
   * Filters out duplicates
   */
  def distinct: Container[T]

  /**
   * Splits stream into two sub-streams
   *
   * The left stream contains all elements as long as `f` is true, all
   * subsequent elements go to the right stream.
   */
  def span(f: T => Boolean): (Container[T], Container[T])

  /**
   * Partitions stream into two sub-stream
   *
   * The left stream contains all elements for which `f` is true, all other
   * elements go to the right stream.
   */
  def partition(f: T => Boolean): (Container[T], Container[T])
}

trait FoldFunctions[T] {
  /**
   * Finds first value for which `f` is true
   */
  def find(f: T => Boolean): ReadChannel[Option[T]]

  /**
   * Aggregates a value
   *
   * After each call of `f`, the current value is produced on the channel
   */
  def foldLeft[U](acc: U)(f: (U, T) => U): ReadChannel[U]

  /**
   * Checks for existence of a value for which `f` is true
   *
   * @note Channels: false as long as `f` returns false, then true
   * @note Aggregates: false as long as no row exists where `f` is true,
   *                   then true
   */
  def exists(f: T => Boolean): ReadChannel[Boolean]

  /**
   * Checks whether `f` is true for all elements
   */
  def forall(f: T => Boolean): ReadChannel[Boolean]

  /**
   * Sums over all elements
   *
   * @see [[foldLeft]]
   */
  def sum[U >: T](implicit num: Numeric[U]): ReadChannel[U] =
    foldLeft(num.zero)(num.plus)
}

trait MapFunctions[Container[_], T] {
  /**
   * Applies `f` on each element
   */
  def map[U](f: T => U): Container[U]

  /**
   * Applies partial function `f` on each element; if the function is not
   * defined for the current input, it is filtered out
   */
  def partialMap[U](f: PartialFunction[T, U]): Container[U]

  /**
   * Applies `f` on each element and flatten its result into the stream
   */
  def flatMap[U](f: T => Container[U]): Container[U]

  /**
   * Take all elements until `ch` produces any value
   */
  def takeUntil(ch: ReadChannel[_]): Container[T]

  /**
   * Equality check
   *
   * @note Channels: Current value is equal to `value`
   * @note Aggregates: All rows are equal to `value`
   */
  def equal(value: T): ReadChannel[Boolean]

  /**
   * Inequality check
   *
   * @see [[equal]]
   */
  def unequal(value: T): ReadChannel[Boolean]
}

/**
 * Combinators for sequential access which allow low-level (albeit read-only)
 * access to the underlying [[Seq]] structure that is used by [[Buffer]] for
 * example.
 */
trait SequentialFunctions[T] {
  /**
   * All rows that are encapsulated in a [[Ref]] object
   */
  def get: Seq[Ref[T]]

  /**
   * All row values that are stored within the [[Ref]] objects
   */
  def values: Seq[T]

  /**
   * Iterates over all row values with the given function `f`
   */
  def foreach(f: T => Unit)
}

trait IterateFunctions[T] {
  /**
   * Stream contains at least one occurrence of `value`.
   *
   * @note Channels: Once true, will never produce any other value.
   * @note Aggregates: When the item is removed, it will produce false.
   */
  def contains(value: T): ReadChannel[Boolean]
}

/**
 * Combinators to associate values to elements; used by [[OrderedMap]]
 */
trait AssocFunctions[A, B] {
  /** Value associated to `key` */
  def value(key: Ref[A]): B

  /** @see [[value]] */
  def apply(key: Ref[A]): B = value(key)

  /** Sets the value of `key` to `value` */
  def update(key: Ref[A], value: B)

  /** Removes `key` */
  def remove(key: Ref[A])

  /** @see [[remove]] */
  def -=(key: Ref[A]) = remove(key)
}

/**
 * As [[Buffer]] is an ordered list, these combinators obtain rows depending
 * on relative or absolute positions
 */
trait OrderFunctions[T] {
  type Index = Int

  /** Row before `handle` */
  def before(handle: Ref[T]): Ref[T]

  /** Row after `handle` */
  def after(handle: Ref[T]): Ref[T]

  /** Row before `handle` with the possibility of non-existence */
  def beforeOption(handle: Ref[T]): Option[Ref[T]]

  /** Row after `handle` with the possibility of non-existence */
  def afterOption(handle: Ref[T]): Option[Ref[T]]

  /** Index of `handle` */
  def indexOf(handle: Ref[T]): Index

  /** Current rows as a channel */
  def toSeq: ReadChannel[Seq[T]]

  /** N-th row */
  def value(index: Index): T

  /** @see [[value]] */
  def apply(index: Index): T = value(index)
}

/** Combinators to operate on the container in its entirety */
trait UpdateFunctions[T] {
  /**
   * @note StateChannels: Change current value (if exists) according to `f`
   * @note Aggregates: Change each row to the value returned by `f`
   */
  def update(f: T => T)

  /**
   * @note StateChannels: Clear current value (if exists)
   * @note Aggregate: Remove all rows
   */
  def clear()
}

trait UpdateSequenceFunctions[Container[_], T] {
  /** Prepends `element` */
  def prepend(element: T): Ref[T]

  /** Appends `element` */
  def append(element: T): Ref[T]

  /** Inserts `element` before `handle` */
  def insertBefore(handle: Ref[T], element: T): Ref[T]

  /** Inserts `element` after `handle` */
  def insertAfter(handle: Ref[T], element: T): Ref[T]

  /** Replaces value of `reference` by `element` */
  def replace(reference: Ref[T], element: T): Ref[T]

  /** Removes row by its reference `handle` */
  def remove(handle: Ref[T])

  /** Appends all elements from `aggregate` */
  def appendAll(aggregate: Container[T])

  /** Removes all elements from `aggregate` */
  def removeAll(aggregate: Container[T])

  /** Replaces contents with `elements` */
  def set(elements: Container[T])

  /** Replaces contents with `elements` */
  def set(elements: Seq[Ref[T]])

  /** @see [[append]] */
  def +=(value: T) = append(value)

  /** @see [[appendAll]] */
  def ++=(aggregate: Container[T]) = appendAll(aggregate)

  /** @see [[remove]] */
  def -=(value: Ref[T]) = remove(value)

  /** @see [[removeAll]] */
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
   * Negation of [[isEmpty]]
   */
  def nonEmpty: ReadChannel[Boolean]
}
