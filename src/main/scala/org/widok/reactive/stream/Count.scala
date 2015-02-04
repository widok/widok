package org.widok.reactive.stream

import org.widok.ReadChannel

trait Count[T] {
  /**
   * Checks for existence of a value for which `f` is true
   *
   * @note Buffers: false as long as no row exists where `f` is true, then true
   * @note Channels: false as long as `f` returns false, then true
   */
  def exists(f: T => Boolean): ReadChannel[Boolean]

  /**
   * Checks whether `f` is true for all elements
   */
  def forall(f: T => Boolean): ReadChannel[Boolean]

  /**
   * Count number of occurrence of `value`.
   *
   * @note Buffers: When the element is removed, the counter is decreased.
   * @note Channels: With every matching element, the counter is increased.
   */
  def count(value: T): ReadChannel[Int]

  /**
   * Count number of elements that are unequal to `value`.
   *
   * @see [[count]]
   */
  def countUnequal(value: T): ReadChannel[Int]

  /**
   * Stream contains at least one occurrence of `value`.
   *
   * @note Buffers: When the item is removed, it will produce false.
   * @note Channels: Once true, will never produce any other value.
   */
  def contains(value: T): ReadChannel[Boolean]

  /**
   * Equality check
   *
   * @note Buffers: All rows are equal to `value`
   * @note Channels: Current value is equal to `value`
   */
  def equal(value: T): ReadChannel[Boolean]

  /**
   * Inequality check
   *
   * @note Buffers: All rows are unequal to `value`
   * @note Channels: Current value is not equal to `value`
   * @see [[equal]]
   */
  def unequal(value: T): ReadChannel[Boolean]
}
