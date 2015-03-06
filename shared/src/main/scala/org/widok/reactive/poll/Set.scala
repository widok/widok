package org.widok.reactive.poll

trait BufSet[T] {
  def toSet$: Set[T]
}
