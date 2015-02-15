package org.widok.reactive.mutate

trait PartialChannel[T] {
  def partialUpdate(f: PartialFunction[T, T])

  /** Clear current value (if exists) */
  def clear()
}
