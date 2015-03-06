package org.widok.reactive.poll

trait PartialChannel {
  /** Value is defined */
  def isDefined$: Boolean

  /** Value is undefined */
  def undefined$: Boolean
}
