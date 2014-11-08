package org.widok

/**
 * An aggregate models a read-only collection of elements as a stream of changes.
 */
object Aggregate {
  trait Position[T] {
    def map[U](f: T => U): Position[U] = {
      this match {
        case Position.Head() => Position.Head[U]()
        case Position.Last() => Position.Last[U]()
        case Position.Before(elem) => Position.Before[U](f(elem))
        case Position.After(elem) => Position.After[U](f(elem))
      }
    }
  }

  object Position {
    case class Head[T]() extends Position[T]
    case class Last[T]() extends Position[T]
    case class Before[T](elem: T) extends Position[T]
    case class After[T](elem: T) extends Position[T]
  }

  trait Change[T]
  object Change {
    case class Insert[T](position: Position[T], handle: T) extends Change[T]
    case class Remove[T](handle: T) extends Change[T]
    case class Clear[T]() extends Change[T]
  }
}

trait Aggregate[T] extends SizeFunctions[T] {
  import Aggregate.Change

  private[widok] val chChanges: Channel[Change[T]]

  def foreach(f: T => Unit)

  /* Downcasts ``chChanges`` in order to drop writing permissions. */
  def changes: ReadChannel[Change[T]] = chChanges

  def currentSize: Int

  def toVarMap[U](default: U): AggMap[T, Var[U]] = VarMap(this, default)

  def size: ReadChannel[Int] =
    chChanges.forkUni { change =>
      Result.Next(Some(currentSize))
    }

  def isEmpty: ReadChannel[Boolean] = size.map(_ == 0)
  def nonEmpty: ReadChannel[Boolean] = size.map(_ != 0)
}
