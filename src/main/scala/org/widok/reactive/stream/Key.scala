package org.widok.reactive.stream

import org.widok.{ReadBuffer, ReadPartialChannel}

trait Key[A, B] {
  /** Observe `key` and encapsulate its value states in a partial channel */
  def value(key: A): ReadPartialChannel[B]

  /** Convert dictionary to buffer */
  def toBuffer: ReadBuffer[(A, B)]
}
