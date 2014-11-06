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

object OptVar {
  def apply[T](value: T): OptVar[T] = {
    val res = OptVar[T]()
    res := value
    res
  }
}

/**
 * Publishes a stream of defined values. Use isEmpty() to detect when the
 * current value is cleared.
 */
case class OptVar[T]() extends StateChannel[T]
                       with SizeFunctions[T]
{
  private var cached: Option[T] = None
  private val defined = LazyVar[Boolean](cached.isDefined)

  attach { t =>
    cached = Some(t)
    defined.produce()
  }

  def isEmpty: ReadChannel[Boolean] = defined.map(!_)
  def nonEmpty: ReadChannel[Boolean] = defined

  def isDefined: ReadChannel[Boolean] = defined

  def flush(f: T => Unit) {
    if (cached.isDefined) f(cached.get)
  }

  def size: ReadChannel[Int] = defined.map(if (_) 1 else 0)

  def clear() {
    cached = None
    defined.produce()
  }

  /** @note This method may only be called if the value is defined. */
  def get: T = cached.get

  override def toString = cached.map(_.toString).getOrElse("<undefined>")
}
