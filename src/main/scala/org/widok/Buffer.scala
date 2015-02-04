package org.widok

import scala.collection.mutable

/**
 * A buffer is a reactive ordered list of elements
 */
object Buffer {
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

  trait Delta[T]
  object Delta {
    case class Insert[T](position: Position[T], element: T) extends Delta[T]
    case class Replace[T](reference: T, element: T) extends Delta[T]
    case class Remove[T](element: T) extends Delta[T]
    case class Clear[T]() extends Delta[T]
  }

  def apply[T](): Buffer[T] = new Buffer[T] { }
  def apply[T](elements: T*): Buffer[T] = from(elements)

  def from[T](elements: Seq[T]) = {
    val buf = Buffer[T]()
    buf.set(elements)
    buf
  }

  def from[T](chgs: ReadChannel[Delta[T]]): Buffer[T] = {
    val buf = Buffer[T]()
    buf.changes << chgs
    buf
  }

  implicit def BufferToSeq[T](buf: Buffer[T]): Seq[T] = buf.elements
}

object DeltaBuffer {
  import Buffer.Delta

  def apply[T](delta: ReadChannel[Delta[T]]): DeltaBuffer[T] =
    new DeltaBuffer[T] {
      override val changes = delta
    }
}

trait DeltaBuffer[T]
  extends reactive.stream.Size
  with reactive.stream.Empty[T]
  with reactive.stream.Count[T]
  with reactive.stream.Map[DeltaBuffer, T]
{
  import Buffer.Delta
  val changes: ReadChannel[Delta[T]]

  def size: ReadChannel[Int] = {
    val count = Var(0)

    changes.attach {
      case Delta.Insert(_, _) => count.update(_ + 1)
      case Delta.Remove(_) => count.update(_ - 1)
      case Delta.Clear() if count.get != 0 => count := 0
      case _ =>
    }

    count
  }

  def isEmpty: ReadChannel[Boolean] = size.map(_ == 0)
  def nonEmpty: ReadChannel[Boolean] = size.map(_ != 0)

  def count(value: T): ReadChannel[Int] = {
    val found = Var(0)

    changes.attach {
      case Delta.Insert(_, `value`) => found.update(_ + 1)
      case Delta.Replace(`value`, _) => found.update(_ - 1)
      case Delta.Replace(_, `value`) => found.update(_ + 1)
      case Delta.Remove(`value`) => found.update(_ - 1)
      case Delta.Clear() if found.get != 0 => found := 0
      case _ =>
    }

    found
  }

  def countUnequal(value: T): ReadChannel[Int] = {
    val unequal = Var(0)

    changes.attach {
      case Delta.Insert(_, v) if v != value => unequal.update(_ + 1)
      case Delta.Replace(_, v) if v != value => unequal.update(_ + 1)
      case Delta.Remove(v) if v != value => unequal.update(_ - 1)
      case Delta.Clear() if unequal.get != 0 => unequal := 0
      case _ =>
    }

    unequal
  }

  def contains(value: T): ReadChannel[Boolean] =
    count(value)
      .map(_ != 0)
      .distinct

  def equal(value: T): ReadChannel[Boolean] =
    countUnequal(value)
      .map(_ == 0)
      .distinct

  def unequal(value: T): ReadChannel[Boolean] =
    count(value)
      .map(_ == 0)
      .distinct

  def exists(f: T => Boolean): ReadChannel[Boolean] = {
    val equal = Var(0)

    changes.attach {
      case Delta.Insert(_, v) if f(v) => equal.update(_ + 1)
      case Delta.Replace(k, v) if f(k) && !f(v) => equal.update(_ - 1)
      case Delta.Replace(k, v) if !f(k) && f(v) => equal.update(_ + 1)
      case Delta.Remove(v) if f(v) => equal.update(_ - 1)
      case Delta.Clear() if equal.get != 0 => equal := 0
      case _ =>
    }

    equal
      .map(_ == 0)
      .distinct
  }

  def forall(f: T => Boolean): ReadChannel[Boolean] = {
    val unequal = Var(0)

    changes.attach {
      case Delta.Insert(_, v) if !f(v) => unequal.update(_ + 1)
      case Delta.Replace(k, v) if f(k) && !f(v) => unequal.update(_ + 1)
      case Delta.Replace(k, v) if !f(k) && f(v) => unequal.update(_ - 1)
      case Delta.Remove(v) if !f(v) => unequal.update(_ - 1)
      case Delta.Clear() if unequal.get != 0 => unequal := 0
      case _ =>
    }

    unequal
      .map(_ == 0)
      .distinct
  }

  def insertions: ReadChannel[T] = changes.partialMap {
    case Delta.Insert(_, element) => element
  }

  /** @note `f` should not be side-effecting */
  def map[U](f: T => U): DeltaBuffer[U] = {
    val chgs: ReadChannel[Delta[U]] = changes.map {
      case Delta.Insert(position, element) =>
        Delta.Insert(position.map(f), f(element))
      case Delta.Replace(reference, element) =>
        Delta.Replace(f(reference), f(element))
      case Delta.Remove(element) =>
        Delta.Remove(f(element))
      case Delta.Clear() =>
        Delta.Clear()
    }

    DeltaBuffer(chgs)
  }

  def buffer: Buffer[T] = {
    val buf = Buffer[T]()
    buf.changes << changes
    buf
  }

  def mapTo[U](f: T => U): DeltaDict[T, U] = {
    // TODO Implement Channel.flatMapSeq(f: T => Seq[T])
    val delta: ReadChannel[Dict.Delta[T, U]] = changes.map[Dict.Delta[T, U]] {
      case Delta.Insert(position, element) =>
        Dict.Delta.Insert(element, f(element))
      case Delta.Replace(reference, element) => ???
        /*(Dict.Delta.Remove(f(reference)),
          Dict.Delta.Insert(element, f(element)))*/
      case Delta.Remove(element) =>
        Dict.Delta.Remove(element)
      case Delta.Clear() =>
        Dict.Delta.Clear()
    }

    DeltaDict(delta)
  }
}

trait StateBuffer[T] extends Disposable {
  import Buffer.Delta
  import Buffer.Position

  private[widok] val elements = mutable.ArrayBuffer.empty[T]

  val changes = new RootChannel[Delta[T]] {
    def flush(f: Delta[T] => Unit) {
      elements.foreach { element =>
        f(Delta.Insert(Position.Last(), element))
      }
    }
  }

  private[widok] val subscription = changes.attach {
    case Delta.Insert(Position.Head(), element) =>
      elements.prepend(element)
    case Delta.Insert(Position.Last(), element) =>
      elements.append(element)
    case Delta.Insert(Position.Before(reference), element) =>
      val position = elements.indexOf(reference)
      assert(position != -1, "insertBefore() with invalid position")
      elements.insert(position, element)
    case Delta.Insert(Position.After(reference), element) =>
      val position = elements.indexOf(reference)
      assert(position != -1, "insertAfter() with invalid position")
      elements.insert(position + 1, element)
    case Delta.Replace(reference, element) =>
      val position = elements.indexOf(reference)
      assert(position != -1, "replace() with invalid position")
      elements(position) = element
    case Delta.Remove(element) =>
      val position = elements.indexOf(element)
      assert(position != -1, "remove() with invalid position")
      elements.remove(position)
    case Delta.Clear() =>
      elements.clear()
  }

  def dispose() {
    subscription.dispose()
  }
}

trait PollBuffer[T]
  extends reactive.poll.Index[Seq, Int, T]
  with reactive.poll.RelativeOrder[T]
  with reactive.poll.Iterate[T]
  with reactive.stream.RelativeOrder[T]
  with reactive.stream.Filter[ReadBuffer, T]
  with reactive.stream.MapExtended[ReadBuffer, T]
  with reactive.stream.AbsoluteOrder[ReadBuffer, T]
{
  import Buffer.Delta
  import Buffer.Position

  val changes: ReadChannel[Delta[T]]

  private[widok] val elements: mutable.ArrayBuffer[T]

  def get: Seq[T] = elements

  def foreach(f: T => Unit) {
    elements.foreach(f)
  }

  /** TODO Could this be implemented more efficiently without iterating over `elements`? */
  def filter(f: T => Boolean): ReadBuffer[T] = {
    val buf = Buffer[T]()

    changes.attach {
      case Delta.Insert(Position.Head(), element) if f(element) =>
        buf.changes := Delta.Insert(Position.Head(), element)

      case Delta.Insert(Position.Last(), element) if f(element) =>
        buf.changes := Delta.Insert(Position.Last(), element)

      case Delta.Insert(Position.Before(reference), element) if f(reference) && f(element) =>
        buf.changes := Delta.Insert(Position.Before(reference), element)

      case Delta.Insert(Position.Before(reference), element) if !f(reference) && f(element) =>
        val insert = elements.drop(indexOf(reference)).find(x => x != element && f(x))
        if (insert.isEmpty) buf.changes := Delta.Insert(Position.Last(), element)
        else buf.changes := Delta.Insert(Position.After(insert.get), element)

      case Delta.Insert(Position.After(reference), element) if f(reference) && f(element) =>
        buf.changes := Delta.Insert(Position.After(reference), element)

      case Delta.Insert(Position.After(reference), element) if !f(reference) && f(element) =>
        val insert = elements.drop(indexOf(reference)).find(x => x != element && f(x))
        if (insert.isEmpty) buf.changes := Delta.Insert(Position.Last(), element)
        else buf.changes := Delta.Insert(Position.Before(insert.get), element)

      case Delta.Replace(reference, element) if f(reference) && f(element) =>
        buf.changes := Delta.Replace(reference, element)

      case Delta.Replace(reference, element) if f(reference) && !f(element) =>
        buf.changes := Delta.Remove(reference)

      case Delta.Replace(reference, element) if !f(reference) && f(element) =>
        val insert = elements.drop(indexOf(reference)).find(x => x != element && f(x))
        if (insert.isEmpty) buf.changes := Delta.Insert(Position.Last(), element)
        else buf.changes := Delta.Insert(Position.Before(insert.get), element)

      case Delta.Remove(element) if f(element) =>
        buf.changes := Delta.Remove(element)

      case Delta.Clear() =>
        buf.changes := Delta.Clear()

      case _ =>
    }

    buf
  }

  def partition(f: T => Boolean): (ReadBuffer[T], ReadBuffer[T]) =
    (filter(f), filter((!(_: Boolean)).compose(f)))

  def update(f: T => T) {
    ???
  }

  def value(index: Int): T = elements(index)
  def indexOf(handle: T): Int = elements.indexOf(handle)
  def toSeq: ReadChannel[Seq[T]] = changes.map(_ => elements)

  def foreach(f: T => Unit) {
    elements.foreach(element => f(element))
  }

  def before(value: T): ReadChannel[T] = ???
  def after(value: T): ReadChannel[T] = ???
  def beforeOption(value: T): PartialChannel[T] = ???
  def afterOption(value: T): PartialChannel[T] = ???

  def before$(value: T): T = {
    val position = indexOf(value) - 1
    elements(position)
  }

  def beforeOption$(value: T): Option[T] = {
    val position = indexOf(value) - 1
    if (position >= 0) Some(elements(position))
    else None
  }

  def after$(value: T): T = {
    val position = indexOf(value) + 1
    elements(position)
  }

  def afterOption$(value: T): Option[T] = {
    val position = indexOf(value) + 1
    if (position < get.size) Some(elements(position))
    else None
  }

  def splitAt(element: T): (ReadBuffer[T], ReadBuffer[T]) = {
    val (left, right) = get.splitAt(elements.indexOf(element))
    (Buffer.from(left), Buffer.from(right))
  }

  def take(count: Int): ReadBuffer[T] = {
    val result = Buffer[T]()
    changes.attach(_ => result.set(get.take(count)))
    result
  }

  def drop(count: Int): ReadBuffer[T] = {
    val result = Buffer[T]()
    changes.attach(_ => result.set(get.drop(count)))
    result
  }

  def head: ReadChannel[T] = {
    val hd = Opt[T]()

    changes.attach {
      case Delta.Insert(Position.Head(), element) => hd := element
      case Delta.Insert(Position.Last(), element)
        if hd.isEmpty$ => hd := element
      case Delta.Insert(Position.Before(before), element)
        if hd.contains$(before) => hd := element
      case Delta.Replace(reference, element)
        if hd.contains$(reference) => hd := element
      case Delta.Remove(element)
        if hd.contains$(element) => hd := elements.head
      case _ =>
    }

    hd
  }

  def last: ReadChannel[T] = {
    val lst = Opt[T]()

    changes.attach {
      case Delta.Insert(Position.Head(), element)
        if lst.isEmpty$ => lst := element
      case Delta.Insert(Position.Last(), element) =>
        lst := element
      case Delta.Insert(Position.After(after), element)
        if lst.contains$(after) => lst := element
      case Delta.Replace(reference, element)
        if lst.contains$(reference) => lst := element
      case Delta.Remove(element)
        if lst.contains$(element) => lst := elements.last
      case _ =>
    }

    lst
  }

  def headOption: ReadPartialChannel[T] = {
    val opt = Opt[T]()
    changes.attach(_ => opt.set(get.headOption))
    opt
  }

  def lastOption: ReadPartialChannel[T] = {
    val opt = Opt[T]()
    changes.attach(_ => opt.set(get.lastOption))
    opt
  }

  def tail: ReadBuffer[T] = {
    val result = Buffer[T]()
    changes.attach(_ => result.set(get.tail))
    result
  }

  def isHead(element: T): ReadChannel[Boolean] =
    headOption.map(_ == Some(element))

  def isLast(element: T): ReadChannel[Boolean] =
    lastOption.map(_ == Some(element))

  def distinct: ReadBuffer[T] = {
    val result = Buffer[T]()
    changes.attach(_ => result.set(get.distinct))
    result
  }

  def span(f: T => Boolean): (ReadBuffer[T], ReadBuffer[T]) = {
    val left = Buffer[T]()
    val right = Buffer[T]()

    changes.attach { _ =>
      val (leftSpan, rightSpan) = get.span(f)

      left.set(leftSpan)
      left.set(rightSpan)
    }

    (left, right)
  }

  def watch[U](lens: T => ReadChannel[U]): ReadBuffer[T] = {
    flatMapCh { t =>
      lens(t).map(x => Some(t))
    }
  }

  def concat(buf: ReadBuffer[T]): ReadBuffer[T] = {
    val res = Buffer[T]()

    changes.merge(buf.changes).attach { _ =>
      res.clear()
      get.foreach(t => res.append(t))
      buf.get.foreach(t => res.append(t))
    }

    res
  }

  def flatMap[U](f: T => ReadBuffer[U]): ReadBuffer[U] = ???

  def partialMap[U](f: PartialFunction[T, U]): ReadBuffer[U] = ???

  def flatMapCh[U](f: T => ReadChannel[Option[U]]): ReadBuffer[U] = {
    val res = Buffer[U]()
    val values = mutable.HashMap.empty[T, Option[U]]
    val attached = mutable.HashMap.empty[T, ReadChannel[Unit]]

    def valueChange(position: Buffer.Position[T],
                    handle: T,
                    value: Option[U])
    {
      if (value.isEmpty) {
        if (values(handle).isDefined)
          res.remove(values(handle).get)
      } else {
        if (values(handle).isDefined) res.replace(values(handle).get, value.get)
        else {
          position match {
            case Position.Head() => res.prepend(value.get)
            case Position.Last() => res.append(value.get)
            case Position.Before(reference) =>
              if (values(reference).isDefined)
                res.insertBefore(values(reference).get, value.get)
              else {
                val insert = get.drop(indexOf(reference)).find(values(_).isDefined)
                if (insert.isEmpty) res.append(value.get)
                else res.insertAfter(values(insert.get).get, value.get)
              }
            case Position.After(reference) =>
              if (values(reference).isDefined)
                res.insertAfter(values(reference).get, value.get)
              else {
                val insert = get.drop(indexOf(reference)).find(values(_).isDefined)
                if (insert.isEmpty) res.append(value.get)
                else res.insertBefore(values(insert.get).get, value.get)
              }
          }
        }
      }

      values += handle -> value
    }

    changes.attach {
      case Delta.Insert(position, element) =>
        // TODO position may change
        values += element -> None
        val ch = f(element)
        attached +=
          element -> ch.attach(value => valueChange(position, element, value))

      case Delta.Remove(element) =>
        attached(element).dispose()
        attached -= element
        if (values(element).isDefined) res.remove(values(element).get)
        values -= element

      case Delta.Replace(reference, element) => ???

      case Delta.Clear() =>
        attached.foreach { case (_, ch) => ch.dispose() }
        attached.clear()
        values.clear()
        res.clear()
    }

    res
  }

  def foldLeft[U](acc: U)(f: (U, T) => U): ReadChannel[U] = ???

  def takeUntil(ch: ReadChannel[_]): ReadBuffer[T] = ???

  /* TODO Must always return the first matching row; if the row gets deleted,
   * should pick the next match
   */
  def find(f: T => Boolean): PartialChannel[T] = ???

  override def toString = get.toString()
}

trait ReadBuffer[T]
  extends DeltaBuffer[T]
  with PollBuffer[T]

trait WriteBuffer[T]
  extends reactive.mutate.Buffer[Seq, T]
{
  import Buffer.Delta
  import Buffer.Position

  val changes: WriteChannel[Delta[T]]

  def prepend(element: T) {
    changes := Delta.Insert(Position.Head(), element)
  }

  def append(element: T) {
    changes := Delta.Insert(Position.Last(), element)
  }

  def insertBefore(reference: T, element: T) {
    changes := Delta.Insert(Position.Before(reference), element)
  }

  def insertAfter(reference: T, element: T) {
    changes := Delta.Insert(Position.After(reference), element)
  }

  def replace(reference: T, element: T) {
    changes := Delta.Replace(reference, element)
  }

  def remove(element: T) {
    changes := Delta.Remove(element)
  }

  def clear() {
    changes := Delta.Clear()
  }

  def set(elements: Seq[T]) {
    clear()
    elements.foreach(append)
  }

  def appendAll(buf: Seq[T]) {
    /** toList needed because Seq[T] may change. */
    buf.toList.foreach(append)
  }

  def removeAll(buf: Seq[T]) {
    /** toList needed because Seq[T] may change. */
    buf.toList.foreach(remove)
  }
}

trait Buffer[T]
  extends ReadBuffer[T]
  with WriteBuffer[T]
  with StateBuffer[T]

trait RefBuf[T] extends Buffer[Ref[T]] {
  /** All row values that are stored within the [[Ref]] objects */
  def values: Seq[T] = elements.map(_.get)

  def insertBefore(reference: Ref[T], element: T): Ref[T] = {
    val handle = Ref[T](element)
    insertBefore(reference, handle)
    handle
  }

  def insertAfter(reference: Ref[T], element: T): Ref[T] = {
    val handle = Ref[T](element)
    insertAfter(reference, handle)
    handle
  }

  def prepend(element: T): Ref[T] = {
    val handle = Ref[T](element)
    prepend(handle)
    handle
  }

  def append(element: T): Ref[T] = {
    val handle = Ref[T](element)
    append(handle)
    handle
  }

  def replace(reference: Ref[T], element: T): Ref[T] = {
    val handle = Ref[T](element)
    replace(reference, handle)
    handle
  }
}
