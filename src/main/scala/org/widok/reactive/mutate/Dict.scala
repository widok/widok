package org.widok.reactive.mutate

trait Dict[A, B] {
  /** Sets the value of `key` to `value` */
  def insert(key: A, value: B)

  /** Removes `key` */
  def remove(key: A)

  /** Removes all elements */
  def clear()

  /** @see [[remove]] */
  def -=(key: A) = remove(key)

  /** @see [[insert]] */
  def +=(key: A, value: B) = insert(key, value)

  /** @see [[insert]] */
  def +=(kv: (A, B)) = insert(kv._1, kv._2)
}
