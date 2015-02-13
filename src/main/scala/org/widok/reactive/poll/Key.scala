package org.widok.reactive.poll

/** Combinators to resolve keys in dictionaries */
trait Key[A, B] {
  /** Value associated to `key` */
  def value$(key: A): B

  /** Value associated to `key` */
  def get(key: A): Option[B]

  /** Convert dictionary to map **/
  def toMap: Map[A, B]

  /** @see [[value$]] */
  def apply(key: A): B = value$(key)
}

