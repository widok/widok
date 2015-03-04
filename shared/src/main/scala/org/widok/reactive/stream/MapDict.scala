package org.widok.reactive.stream

trait MapDict[Container[_, _], A, B] {
  /** Applies `f` on each value */
  def map[C](f: (A, B) => C): Container[A, C]

  /** Applies `f` on each key */
  def mapKeys[C](f: A => C): Container[C, B]
}
