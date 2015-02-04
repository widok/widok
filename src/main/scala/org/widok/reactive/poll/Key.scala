package org.widok.reactive.poll

/**
 * Query by key
 * Combinators to associate values to elements; used by [[OrderedMap]]
 */
trait Key[A, B] {
  /** Value associated to `key` */
  def value(key: A): B
  def get(key: A): Option[B]

  /** @see [[value]] */
  def apply(key: A): B = value(key)
}

