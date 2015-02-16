package org.widok.reactive.stream

trait Aggregate[Container[_], T] {
  /** Filters out (merges) duplicates */
  def distinct: Container[T]
}
