package org.widok.reactive.mutate

trait BufSet[T] {
  /** Adds `key` */
  def insert(key: T)

  /** Insert `keys` */
  def insertAll(keys: Seq[T])

  /** Removes `key` */
  def remove(key: T)

  /** Remove `keys` */
  def removeAll(keys: Seq[T])

  /** Replace all elements with `keys` */
  def set(keys: Seq[T])

  /** Removes all elements */
  def clear()

  /** @see [[remove]] */
  def -=(key: T) = remove(key)

  /** @see [[insert]] */
  def +=(key: T) = insert(key)

  /** @see [[insertAll]] */
  def ++=(keys: Seq[T]) = insertAll(keys)

  /** @see [[removeAll]] */
  def --=(keys: Seq[T]) = removeAll(keys)
}
