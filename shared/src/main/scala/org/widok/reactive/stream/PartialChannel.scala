package org.widok.reactive.stream

import org.widok.ReadChannel

trait PartialChannel[T] {
  /** true if partial channel has a value, false otherwise */
  def isDefined: ReadChannel[Boolean]

  /** Filters out all defined values */
  def values: ReadChannel[T]
}
