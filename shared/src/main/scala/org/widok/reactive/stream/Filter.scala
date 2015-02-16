package org.widok.reactive.stream

import org.widok.ReadChannel

trait Filter[Container[_] <: Size, A, B] {
  /** Only include elements for which `f` is true */
  def filter(f: B => Boolean): Container[A]

  def filterNot(f: B => Boolean): Container[A] =
    filter((!(_: Boolean)).compose(f))

  /**
   * All elements are equal to `value`
   *
   * @see [[any]]
   */
  def all(value: B): ReadChannel[Boolean] = filterNot(_ == value).isEmpty

  /**
   * At least one element is equal to `value`
   *
   * @see [[all]]
   */
  def any(value: B): ReadChannel[Boolean] = filter(_ == value).nonEmpty

  /**
   * Checks for existence of a value for which `f` is true
   *
   * @note Buffers: false as long as no row exists where `f` is true, then true
   * @note Channels: false as long as `f` returns false, then true
   *
   * @see [[forall]]
   */
  def exists(f: B => Boolean): ReadChannel[Boolean] = filter(f).nonEmpty

  /**
   * Checks whether `f` is true for all elements
   *
   * @see [[exists]]
   */
  def forall(f: B => Boolean): ReadChannel[Boolean] = filterNot(f).isEmpty

  /**
   * Count number of occurrence of `value`.
   *
   * @note Buffers: When the element is removed, the counter is decreased.
   * @note Channels: With every matching element, the counter is increased.
   */
  def count(value: B): ReadChannel[Int] = filter(_ == value).size

  /**
   * Stream contains at least one occurrence of `value`.
   *
   * @note Buffers: When the item is removed, it will produce false.
   * @note Channels: Once true, will never produce any other value.
   */
  def contains(value: B): ReadChannel[Boolean] = filter(_ == value).nonEmpty

  /**
   * Partitions stream into two sub-stream
   *
   * The left stream contains all elements for which `f` is true, all other
   * elements go to the right stream.
   */
  def partition(f: B => Boolean): (Container[A], Container[A]) =
    (filter(f), filterNot(f))
}
