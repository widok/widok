package org.widok

case class Var[T](private var v: T) extends StateChannel[T] {
  attach(v = _)

  def flush(f: T => Unit) { f(v) }
  def get: T = v
  override def toString = v.toString
}

/**
 * Upon each subscription, emit ``v``. ``v`` is evaluated
 * lazily. In Rx terms LazyVar() can be considered a cold observable.
 */
object LazyVar {
  def apply[T](v: => T) = new StateChannel[T] {
    def flush(f: T => Unit) { f(v) }
    def produce() { this := v }
    override def toString = v.toString
  }
}
