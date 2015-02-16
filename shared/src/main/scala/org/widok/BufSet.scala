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
  with reactive.stream.Filter[ReadBufSet, T, T]
{
  import BufSet.Delta
  val changes: ReadChannel[Delta[T]]

  def size: ReadChannel[Int] = {
    val count = Var(0)

    changes.attach {
      case Delta.Insert(_) => count.update(_ + 1)
      case Delta.Remove(_) => count.update(_ - 1)
      case Delta.Clear() if count.get != 0 => count := 0
    }

    count
  }

  def filter(f: T => Boolean): ReadBufSet[T] = {
    val result = BufSet[T]()

    changes.attach {
      case Delta.Insert(value) if f(value) => result += value
      case Delta.Remove(value) if result.contains$(value) => result -= value
      case Delta.Clear() if result.nonEmpty$ => result.clear()
    }

    result
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
    case Delta.Insert(value) =>
      assert(!elements.contains(value), "Element does not occur")
      elements += value
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
  with reactive.poll.Empty
{
  import BufSet.Delta

  private[widok] val elements: mutable.HashSet[T]

  val changes: ReadChannel[Delta[T]]

  def isEmpty$: Boolean = elements.isEmpty
  def nonEmpty$: Boolean = elements.nonEmpty
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
