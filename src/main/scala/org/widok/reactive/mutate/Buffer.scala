package org.widok.reactive.mutate

trait Buffer[Container[_], T] {
  /** Prepends `element` */
  def prepend(element: T)

  /** Appends `element` */
  def append(element: T)

  /** Inserts `element` before `handle` */
  def insertBefore(handle: T, element: T)

  /** Inserts `element` after `handle` */
  def insertAfter(handle: T, element: T)

  /** Replaces value of `reference` by `element` */
  def replace(reference: T, element: T)

  /** Removes row by its reference `handle` */
  def remove(handle: T)

  /** Appends all elements from `buf` */
  def appendAll(buf: Container[T])

  /** Removes all elements from `buf` */
  def removeAll(buf: Container[T])

  /** Replaces contents with `elements` */
  def set(elements: Container[T])

  /** Remove all rows */
  def clear()

  /** @see [[append]] */
  def +=(value: T) = append(value)

  /** @see [[appendAll]] */
  def ++=(buf: Container[T]) = appendAll(buf)

  /** @see [[remove]] */
  def -=(value: T) = remove(value)

  /** @see [[removeAll]] */
  def --=(agg: Container[T]) = removeAll(agg)
}
