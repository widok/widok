package org.widok.reactive.poll

trait Iterate[T] {
  /** Iterates over all row values with the given function `f` */
  def foreach(f: T => Unit)
}
