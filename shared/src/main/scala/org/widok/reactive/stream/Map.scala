package org.widok.reactive.stream

trait Map[Container[_], T] {
  /** Applies `f` on each element */
  def map[U](f: T => U): Container[U]
}
