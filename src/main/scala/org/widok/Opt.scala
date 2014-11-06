package org.widok

object Opt {
  def apply[T](value: T): Opt[T] = {
    val res = Opt[T]()
    res := value
    res
  }
}

/**
 * Publishes a stream of defined values. Use isEmpty() to detect when the
 * current value is cleared.
 */
case class Opt[T]() extends StateChannel[T] with SizeFunctions[T] {
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
