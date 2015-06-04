package org.widok.reactive.mutate

trait BufSet[T] {
  /** Adds `value` */
  def insert(value: T)

  /** Insert `values` */
  def insertAll(values: Set[T])

  /** Removes `value` */
  def remove(value: T)

  /** Remove `values` */
  def removeAll(values: Set[T])

  /** Insert all elements from `values` if state true, otherwise remove */
  def toggle(state: Boolean, values: T*)

  /** Replace all elements with `values` */
  def set(values: Set[T])

  /** Removes all elements */
  def clear()

  /** @see [[remove]] */
  def -=(value: T) = remove(value)

  /** @see [[insert]] */
  def +=(value: T) = insert(value)

  /** @see [[insertAll]] */
  def ++=(values: Set[T]) = insertAll(values)

  /** @see [[removeAll]] */
  def --=(values: Set[T]) = removeAll(values)
}
