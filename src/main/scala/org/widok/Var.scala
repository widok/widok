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
 * Upon each subscription, emit `v`. `v` is evaluated lazily.
 */
object LazyVar {
  def apply[T](v: => T) = new StateChannel[T]
    with ChannelDefaultSize[T]
  {
    def get: T = v
    def flush(f: T => Unit) { f(v) }
    // TODO Should not provide produce(T), only produce()
    def produce() { this := v }
    def isEmpty: ReadChannel[Boolean] = Var(false)
    def nonEmpty: ReadChannel[Boolean] = Var(true)

    override def toString = s"LazyVar(${v.toString})"
  }
}

/** Every produced value on the channel `change` indicates that the underlying
  * variable was modified and the current value can be retrieved via `get`.
  * If a value v is produced on the resulting channel instead, then set(v) is
  * called.
  */
object PtrVar {
  def apply[T](change: ReadChannel[_], get: => T, set: T => Unit) = new StateChannel[T]
    with ChannelDefaultSize[T]
  {
    val sub = attach(set)
    change.attach(_ => produce())

    def flush(f: T => Unit) { f(get) }
    def produce() { produce(get, sub) }
    def isEmpty: ReadChannel[Boolean] = Var(false)
    def nonEmpty: ReadChannel[Boolean] = Var(true)

    override def toString = s"PtrVar(${get.toString})"
  }
}
