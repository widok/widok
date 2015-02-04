package org.widok.reactive.poll

trait Count[T] {
  def contains$(value: T): Boolean
}
