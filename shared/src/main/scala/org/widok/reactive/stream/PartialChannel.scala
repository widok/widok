package org.widok.reactive.stream

import org.widok.ReadChannel

trait PartialChannel[T] {
  /** true if partial channel has a value, false otherwise */
  def isDefined: ReadChannel[Boolean]

  /** Wraps the defined value or undefined state into an [[Option]] instance */
  def values: ReadChannel[Option[T]]
}
