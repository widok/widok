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
    res := Some(value)
    res
  }
}

case class OptVar[T]() extends StateChannel[Option[T]]
                       with SizeFunctions[T]
{
  protected var cached: Option[T] = None

  attach(t => cached = t)

  def isEmpty: ReadChannel[Boolean] = {
    val res = LazyVar[Boolean](cached.isEmpty)
    attach(_ => res.produce())
    res
  }

  def flush(f: Option[T] => Unit) { f(cached) }
  def nonEmpty: ReadChannel[Boolean] = isEmpty.map(!_)
  def size: ReadChannel[Int] = isEmpty.map(if (_) 0 else 1)
  def values: ReadChannel[T] = partialMap { case Some(v) => v }
  def clear() { this := None }
  def get: Option[T] = cached
  override def toString = cached.map(_.toString).getOrElse("<undefined>")
}
