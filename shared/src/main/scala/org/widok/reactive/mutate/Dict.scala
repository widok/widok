package org.widok.reactive.mutate

trait Dict[A, B] {
  /**
   * Inserts a new key-value tuple. If `key` already exists, [[update]] must
   * be used instead.
   */
  def insert(key: A, value: B)

  /** Sets the value of `key` to `value` */
  def update(key: A, value: B)

  /** Insert all elements from map */
  def insertAll(map: Map[A, B])

  /** Removes `key` */
  def remove(key: A)

  /** Remove all keys from `keys` */
  def removeAll(keys: Seq[A])

  /** Removes all elements */
  def clear()

  /** Replace current dictionary with elements from `map` */
  def set(map: Map[A, B])

  /** @see [[remove]] */
  def -=(key: A) = remove(key)

  /** @see [[removeAll]] */
  def --=(keys: Seq[A]) = removeAll(keys)

  /** @see [[insert]] */
  def +=(key: A, value: B) = insert(key, value)

  /** @see [[insert]] */
  def +=(kv: (A, B)) = insert(kv._1, kv._2)

  /** @see [[insertAll]] */
  def ++=(map: Map[A, B]) = insertAll(map)
}
