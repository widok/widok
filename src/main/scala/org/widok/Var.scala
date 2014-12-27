package org.widok

case class Var[T](private var v: T) extends StateChannel[T] with ChannelDefaultSize[T] {
  attach(v = _)

  def flush(f: T => Unit) { f(v) }
  def get: T = v
  def isEmpty: ReadChannel[Boolean] = Var(false)
  def nonEmpty: ReadChannel[Boolean] = Var(true)

  override def toString = s"Var(${v.toString})"
}

/**
 * Upon each subscription, emit ``v``. ``v`` is evaluated
 * lazily. In Rx terms LazyVar() can be considered a cold observable.
 */
object LazyVar {
  def apply[T](v: => T) = new StateChannel[T]
    with ChannelDefaultSize[T]
  {
    def get: T = v
    def flush(f: T => Unit) { f(v) }
    def produce() { this := v }
    def isEmpty: ReadChannel[Boolean] = Var(false)
    def nonEmpty: ReadChannel[Boolean] = Var(true)

    override def toString = s"LazyVar(${v.toString})"
  }
}
