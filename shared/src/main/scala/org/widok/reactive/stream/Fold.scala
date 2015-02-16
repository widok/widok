package org.widok.reactive.stream

import org.widok.ReadChannel

trait Fold[T] {
  /**
   * Aggregates a value
   *
   * After each call of `f`, the current value is produced on the channel
   */
  def foldLeft[U](acc: U)(f: (U, T) => U): ReadChannel[U]

  // TODO Add biFold() that also deals with deletions

  /**
   * Sums over all elements
   *
   * @see [[foldLeft]]
   */
  def sum[U >: T](implicit num: Numeric[U]): ReadChannel[U] =
    foldLeft(num.zero)(num.plus)

  /**
   * Calculates product
   *
   * @see [[foldLeft]]
   */
  def product[U >: T](implicit num: Numeric[U]): ReadChannel[U] =
    foldLeft(num.one)(num.times)

  /**
   * Calculates minimum value
   *
   * @see [[foldLeft]]
   */
  def min[U >: T](init: U)(implicit num: Numeric[U]): ReadChannel[U] =
    foldLeft(init)(num.min)

  /**
   * Calculates maximum value
   *
   * @see [[foldLeft]]
   */
  def max[U >: T](init: U)(implicit num: Numeric[U]): ReadChannel[U] =
    foldLeft(init)(num.max)
}
