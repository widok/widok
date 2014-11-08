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
case class Opt[T]() extends StateChannel[T] {
  private var cached: Option[T] = None
  private val defined = LazyVar[Boolean](cached.isDefined)

  attach { t =>
    val prevDefined = cached.isDefined
    cached = Some(t)
    if (!prevDefined) defined.produce()
  }

  def isEmpty: ReadChannel[Boolean] = defined.map(!_)
  def nonEmpty: ReadChannel[Boolean] = defined

  def isDefined: ReadChannel[Boolean] = defined

  def flush(f: T => Unit) {
    if (cached.isDefined) f(cached.get)
  }

  def size: ReadChannel[Int] =
    defined.flatMap { state =>
      if (!state) Var(0)
      else foldLeft(0) { case (acc, cur) => acc + 1 }
    }

  def clear() {
    val prevDefined = cached.isDefined
    cached = None
    if (prevDefined) defined.produce()
  }

  /** @note This method may only be called if the value is defined. */
  def get: T = cached.get

  private def str = cached.map(_.toString).getOrElse("<undefined>")
  override def toString = s"Opt($str)"
}
