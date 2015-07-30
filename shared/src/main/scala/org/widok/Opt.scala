package org.widok

trait ReadPartialChannel[T]
  extends ReadChannel[Option[T]]
  with ReadStateChannel[Option[T]]
  with reactive.poll.Empty
  with reactive.poll.Count[T]
  with reactive.stream.PartialChannel[T]
{
  def values: ReadChannel[T] =
    forkUni {
      case None        => Result.Next()
      case Some(value) => Result.Next(value)
    }
}

trait PartialChannel[T]
  extends StateChannel[Option[T]]
  with ReadPartialChannel[T]

/**
 * Publishes a stream of defined values. Use isEmpty() to detect when the
 * current value is cleared.
 */
sealed class Opt[T](private var v: Option[T] = None)
  extends PartialChannel[T]
  with reactive.poll.PartialChannel
  with reactive.mutate.PartialChannel[T]
{
  attach(v = _)

  def isEmpty$: Boolean = v.isEmpty
  def nonEmpty$: Boolean = v.nonEmpty

  def isDefined$: Boolean = v.isDefined
  def undefined$: Boolean = v.isEmpty

  def contains$(value: T): Boolean = v.contains(value)

  def isDefined: ReadChannel[Boolean] = isNot(None)
  def undefined: ReadChannel[Boolean] = is(None)

  def flush(f: Option[T] => Unit) { f(v) }

  def size: ReadChannel[Int] =
    foldLeft(0) {
      case (acc, Some(_)) => acc + 1
      case (acc, None)    => 0
    }

  def clear() { produce(None) }

  def partialUpdate(f: PartialFunction[T, T]) {
    v.foreach(value => produce(f.lift(value)))
  }

  def get: Option[T] = v

  def orElse(default: => ReadChannel[T]): ReadChannel[T] =
    flatMap {
      case None        => default
      case Some(value) => Var(value)
    }

  private def str = get.map(_.toString).getOrElse("<undefined>")
  override def toString = s"Opt($str)"
}

object Opt {
  def apply[T](): Opt[T] = new Opt()
  def apply[T](value: T): Opt[T] = new Opt(Some(value))
}
