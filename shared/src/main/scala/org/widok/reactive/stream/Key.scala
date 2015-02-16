package org.widok.reactive.stream

import org.widok.{DeltaBufSet, ReadPartialChannel}

trait Key[A, B] {
  /** Observes `key` and encapsulate its value states in a partial channel */
  def value(key: A): ReadPartialChannel[B]

  /** Returns key set */
  def keys: DeltaBufSet[A]
}
