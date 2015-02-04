package org.widok.reactive.poll

/** These operations obtain rows depending on relative positions in a list */
trait RelativeOrder[T] {
  /** Row before `value` */
  def before$(value: T): T

  /** Row after `value` */
  def after$(value: T): T

  /** Row before `value` with the possibility of non-existence */
  def beforeOption$(value: T): Option[T]

  /** Row after `value` with the possibility of non-existence */
  def afterOption$(value: T): Option[T]
}
