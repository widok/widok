package org.widok.reactive.stream

import org.widok.ReadPartialChannel

trait Key[A, B] {
  /** Observe `key` and encapsulate its value states in a partial channel */
  def value(key: A): ReadPartialChannel[B]
}
