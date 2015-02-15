package org.widok

import scala.collection.mutable

/**
 * Reactive set
 */
object BufSet {
  sealed trait Delta[T]
  object Delta {
    case class Insert[T](value: T) extends Delta[T]
    case class Remove[T](value: T) extends Delta[T]
    case class Clear[T]() extends Delta[T]
  }

  def apply[T](): BufSet[T] = new BufSet[T] { }

  implicit def BufSetToSeq[T](buf: BufSet[T]): Seq[T] = buf.elements.toSeq
}

object DeltaBufSet {
  import BufSet.Delta

  def apply[T](delta: ReadChannel[Delta[T]]): DeltaBufSet[T] =
    new DeltaBufSet[T] {
      override val changes = delta
    }
}

trait DeltaBufSet[T]
  extends reactive.stream.Size
  with reactive.stream.Empty
  with reactive.stream.Count[T]
{
  import BufSet.Delta
  val changes: ReadChannel[Delta[T]]

  def size: ReadChannel[Int] = {
    var values = mutable.HashSet.empty[T]
    val count = LazyVar(values.size)

    changes.attach {
      case Delta.Insert(k) if !values.contains(k) =>
        values += k
        count.produce()
      case Delta.Remove(k) =>
        values -= k
        count.produce()
      case Delta.Clear() if values.nonEmpty =>
        count.produce()
      case _ =>
    }

    count
  }

  def isEmpty: ReadChannel[Boolean] = size.map(_ == 0)
  def nonEmpty: ReadChannel[Boolean] = size.map(_ != 0)

  def exists(f: T => Boolean): ReadChannel[Boolean] = ???
  def count(value: T): ReadChannel[Int] = ???
  def unequal(value: T): ReadChannel[Boolean] = ???

  def contains(value: T): ReadChannel[Boolean] = {
    val state = Var(false)

    changes.attach {
      case Delta.Insert(k) if k == value && !state.get => state := true
      case Delta.Remove(k) if k == value && state.get => state := false
      case Delta.Clear() if !state.get => state := false
      case _ =>
    }

    state
  }

  def equal(value: T): ReadChannel[Boolean] = ???
  def countUnequal(value: T): ReadChannel[Int] = ???

  def forall(f: T => Boolean): ReadChannel[Boolean] = {
    var values = mutable.HashSet.empty[T]
    val unequal = Var(0)

    changes.attach {
      case Delta.Insert(k) if values.contains(k) && f(k) =>
        unequal.update(_ - 1)
        values -= k
      case Delta.Insert(k) if !values.contains(k) && !f(k) =>
        unequal.update(_ + 1)
        values += k
      case Delta.Remove(k) if values.contains(k) =>
        unequal.update(_ - 1)
        values -= k
      case Delta.Clear() if unequal.get != 0 =>
        unequal := 0
      case _ =>
    }

    unequal
      .map(_ == 0)
      .distinct
  }
}

trait StateBufSet[T] extends Disposable {
  import BufSet.Delta

  private[widok] val elements = mutable.HashSet.empty[T]

  val changes = new RootChannel[Delta[T]] {
    def flush(f: Delta[T] => Unit) {
      elements.foreach(value => f(Delta.Insert(value)))
    }
  }

  private[widok] val subscription = changes.attach {
    case Delta.Insert(value) => elements += value
    case Delta.Remove(value) => elements -= value
    case Delta.Clear() => elements.clear()
  }

  def dispose() {
    subscription.dispose()
  }
}

trait WriteBufSet[T]
  extends reactive.mutate.BufSet[T]
{
  import BufSet.Delta

  val changes: Channel[Delta[T]]

  def insert(value: T) {
    changes := Delta.Insert(value)
  }

  def insertAll(values: Seq[T]) {
    values.foreach(insert)
  }

  def remove(value: T) {
    changes := Delta.Remove(value)
  }

  def removeAll(values: Seq[T]) {
    values.foreach(remove)
  }

  def set(values: Seq[T]) {
    clear()
    insertAll(values)
  }

  def toggle(state: Boolean, values: T*) {
    if (state) insertAll(values)
    else removeAll(values)
  }

  def clear() {
    changes := Delta.Clear()
  }
}

trait PollBufSet[T]
  extends reactive.poll.Count[T]
{
  import BufSet.Delta

  private[widok] val elements: mutable.HashSet[T]

  val changes: ReadChannel[Delta[T]]

  def contains$(value: T): Boolean = elements.contains(value)

  def toSeq: ReadChannel[Seq[T]] = changes.map(_ => elements.toSeq)
}

trait ReadBufSet[T]
  extends PollBufSet[T]
  with DeltaBufSet[T]

trait BufSet[T]
  extends ReadBufSet[T]
  with WriteBufSet[T]
  with StateBufSet[T]
