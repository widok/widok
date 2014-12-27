package org.widok

import scala.collection.mutable

/**
 * An aggregate models a read-only collection of elements as a stream of changes.
 */
object Aggregate {
  trait Position[T] {
    def map[U](f: T => U): Position[U] = {
      this match {
        case Position.Head() => Position.Head[U]()
        case Position.Last() => Position.Last[U]()
        case Position.Before(reference) => Position.Before[U](f(reference))
        case Position.After(reference) => Position.After[U](f(reference))
      }
    }
  }

  object Position {
    case class Head[T]() extends Position[T]
    case class Last[T]() extends Position[T]
    case class Before[T](reference: T) extends Position[T]
    case class After[T](reference: T) extends Position[T]
  }

  trait Change[T]
  object Change {
    case class Insert[T](position: Position[T], element: T) extends Change[T]
    case class Remove[T](element: T) extends Change[T]
    case class Clear[T]() extends Change[T]
  }
}

trait Aggregate[T]
  extends SizeFunctions
  with FoldFunctions[T]
  with IterateFunctions[T]
  with SequentialFunctions[T]
{
  import Aggregate.Change

  /* TODO Downcast to ReadChannel in order to drop writing permissions. */
  val changes: Channel[Change[Ref[T]]]

  def size: ReadChannel[Int] =
    changes.forkUniState(change =>
      Result.Next(Some(get.size)),
      Some(get.size)
    ).distinct

  def isEmpty: ReadChannel[Boolean] =
    changes.forkUniState(
      change => Result.Next(Some(get.isEmpty)),
      Some(get.isEmpty)
    ).distinct

  def nonEmpty: ReadChannel[Boolean] =
    changes.forkUniState(change =>
      Result.Next(Some(get.nonEmpty)),
      Some(get.nonEmpty)
    ).distinct

  /* TODO Instantiate FoldFunctions[Ref[T]] */
  def find(f: T => Boolean): ReadChannel[Option[T]] = ???

  def foldLeft[U](acc: U)(f: (U, T) => U): ReadChannel[U] = ???

  def exists(f: T => Boolean): ReadChannel[Boolean] = {
    val matching = new mutable.HashSet[Ref[T]]()
    val state = LazyVar(matching.nonEmpty)

    changes.attach {
      case Change.Insert(position, element) =>
        if (f(element.get)) {
          matching.add(element)
          state.produce()
        }

      case Change.Remove(element) =>
        if (matching.contains(element)) {
          matching.remove(element)
          state.produce()
        }

      case Change.Clear() =>
        matching.clear()
        state.produce()
    }

    state
  }

  def forall(f: T => Boolean): ReadChannel[Boolean] = {
    val falseIds = new mutable.HashSet[Ref[T]]()
    val state = LazyVar(falseIds.isEmpty)

    changes.attach {
      case Change.Insert(_, element) =>
        if (!f(element.get)) {
          falseIds.add(element)
          state.produce()
        }

      case Change.Remove(element) =>
        if (falseIds.contains(element)) {
          falseIds.remove(element)
          state.produce()
        }

      case Change.Clear() =>
        falseIds.clear()
        state.produce()
    }

    state
  }

  def watch(reference: Ref[T]): ReadChannel[Option[Ref[T]]] =
    changes.partialMap {
      /* TODO Support value changes via Change.Update */
      case Change.Insert(_, element) if element == reference => Some(element)
      case Change.Remove(element) if element == reference => None
      case Change.Clear() => None
    }
}
