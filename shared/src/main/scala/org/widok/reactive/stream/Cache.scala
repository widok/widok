package org.widok.reactive.stream

import org.widok.{ReadStateChannel, ReadPartialChannel}

trait Cache[T] {
  def cache: ReadPartialChannel[T]
  def cache(default: T): ReadStateChannel[T]
}
