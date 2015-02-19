package org.widok.reactive.stream

import org.widok.ReadPartialChannel

trait Find[T] {
  /** Finds first value for which `f` is true */
  def find(f: T => Boolean): ReadPartialChannel[T]
}
