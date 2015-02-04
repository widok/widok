package org.widok.reactive.poll

trait Filter[Container[_], T] {
  /** Only include elements for which `f` is true */
  def filter$(f: T => Boolean): Container[T]

  /** Filters out duplicates */
  def distinct$: Container[T]

  /**
   * Split container into two sub-containers
   *
   * The left container contains all elements if `f` is true, all subsequent
   * elements go to the right container.
   */
  def span$(f: T => Boolean): (Container[T], Container[T])

  /**
   * Partitions stream into two sub-stream
   *
   * The left stream contains all elements for which `f` is true, all other
   * elements go to the right stream.
   */
  def partition$(f: T => Boolean): (Container[T], Container[T])

  /** Finds first value for which `f` is true */
  def find$(f: T => Boolean): Option[T]
}
