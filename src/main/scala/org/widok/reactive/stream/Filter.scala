package org.widok.reactive.stream

import org.widok.ReadChannel

trait Filter[Container[_], T] {
  /** Take all elements until `ch` produces any value */
  def takeUntil(ch: ReadChannel[_]): Container[T]

  /** Only include elements for which `f` is true */
  def filter(f: T => Boolean): Container[T]

  /** Filters out duplicates */
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

  /** Finds first value for which `f` is true */
  def find(f: T => Boolean): PartialChannel[T]
}
