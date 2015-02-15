package org.widok.reactive.poll

/** Combinators to resolve keys in dictionaries */
trait Key[A, B] {
  /** Value associated to `key` */
  def value$(key: A): B

  /** Value associated to `key` */
  def get(key: A): Option[B]

  /** Returns key set */
  def keys$: Set[A]

  /** Returns all values */
  def values$: Iterable[B]

  /** Converts dictionary to map **/
  def toMap: Map[A, B]

  /** @see [[value$]] */
  def apply(key: A): B = value$(key)
}

