package org.widok.reactive.propagate

trait Channel[T] {
  def produce(value: T)
  def :=(value: T) = produce(value)
}
