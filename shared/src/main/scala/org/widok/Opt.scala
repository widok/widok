package org.widok

trait ReadPartialChannel[T]
  extends ReadChannel[T]
  with ReadStateChannel[T]
  with reactive.poll.Empty
  with reactive.poll.Count[T]
  with reactive.stream.PartialChannel[T]

trait PartialChannel[T]
  extends StateChannel[T]
  with ReadPartialChannel[T]

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
case class Opt[T]()
  extends PartialChannel[T]
  with reactive.mutate.PartialChannel[T]
{
  private var cached: Option[T] = None
  private val defined = LazyVar[Boolean](cached.isDefined)

  attach { t =>
    val prevDefined = cached.isDefined
    cached = Some(t)
    if (!prevDefined) defined.produce()
  }

  def isEmpty$: Boolean = cached.isEmpty
  def nonEmpty$: Boolean = cached.nonEmpty
  def contains$(value: T): Boolean = cached.contains(value)

  def isEmpty: ReadChannel[Boolean] = defined.map(!_)
  def nonEmpty: ReadChannel[Boolean] = defined

  def isDefined: ReadChannel[Boolean] = defined

  def flush(f: T => Unit) {
    if (cached.isDefined) f(cached.get)
  }

  // TODO This does not work.
  //def size: ReadChannel[Int] =
  //  defined.flatMap { state =>
  //    if (!state) Var(0)
  //    else foldLeft(0) { case (acc, cur) => acc + 1 }
  //  }

  // Workaround
  def size: ReadChannel[Int] = {
    var count = 0
    val res = forkUniState(t => {
      count += 1
      Result.Next(Some(count))
    }, Some(count)).asInstanceOf[ChildChannel[Int, Int]]
    defined.attach(d ⇒ if (!d) {
      count = 0
      res := 0
    })
    res
  }

  def clear() {
    val prevDefined = cached.isDefined
    cached = None
    if (prevDefined) defined.produce()
  }

  def set(value: Option[T]) {
    if (value.isDefined) this := value.get
    else clear()
  }

  def partialUpdate(f: PartialFunction[T, T]) = ???

  /** @note This method may only be called if the value is defined. */
  def get: T = cached.get

  def getOrElse(default: => T): T = cached.getOrElse(default)
  def orElse(default: => ReadChannel[T]): ReadChannel[T] =
    defined.flatMap { value =>
      if (value) Var(cached.get)
      else default
    }

  def values: ReadChannel[Option[T]] =
    defined
      .partialMap { case false => Option.empty[T] }
      .merge(map(Some(_)))

  def toOption: Option[T] = cached

  private def str = cached.map(_.toString).getOrElse("<undefined>")
  override def toString = s"Opt($str)"
}
