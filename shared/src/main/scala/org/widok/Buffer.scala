package org.widok

import scala.collection.mutable

/**
 * A buffer is a reactive ordered list of elements
 */
object Buffer {
  sealed trait Position[T] {
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

  sealed trait Delta[T]
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

  implicit def ReadBufferToSeq[T](buf: ReadBuffer[T]): Seq[T] = buf.elements

  implicit def flatten[T](buf: ReadBuffer[ReadBuffer[T]]): ReadBuffer[T] = {
    /** TODO Find a more efficient implementation */
    val result = Buffer[T]()
    val attached = mutable.HashMap.empty[ReadBuffer[T], ReadChannel[Unit]]

    def refresh() {
      result.clear()
      buf.get.foreach(result ++= _.get)
    }

    buf.changes.attach {
      case Delta.Insert(position, element) =>
        attached += element -> element.changes.attach(_ => refresh())
        refresh()
      case Delta.Replace(reference, element) =>
        attached(reference).dispose()
        attached -= reference
        attached += element -> element.changes.attach(_ => refresh())
        refresh()
      case Delta.Remove(element) =>
        attached(element).dispose()
        attached -= element
        refresh()
      case Delta.Clear() =>
        attached.clear()
        result.clear()
    }

    result
  }
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

  def takeUntil(ch: ReadChannel[_]): DeltaBuffer[T] =
    DeltaBuffer(changes.takeUntil(ch))

  def insertions: ReadChannel[T] = changes.partialMap {
    case Delta.Insert(_, element) => element
  }

  def removals: ReadChannel[T] = changes.partialMap {
    case Delta.Remove(element) => element
  }

  /** Unlike `mapPure`, stores mapping locally. This is currently needed as
    * widget objects are not immutable.
    */
  def map[U](f: T => U): DeltaBuffer[U] = {
    val mapping = mutable.ArrayBuffer.empty[(T, U)]
    def cached(key: T): U = mapping.find { case (k, _) => k == key }.get._2
    def remove(key: T) = mapping.remove(
      mapping.indexWhere { case (k, _) => k == key }
    )
    def replace(key: T, value: (T, U)) = mapping.update(
      mapping.indexWhere { case (k, _) => k == key },
      value
    )

    val chgs: ReadChannel[Delta[U]] = changes.map {
      case Delta.Insert(position, element) =>
        val mapped = f(element)
        val res = Delta.Insert(position.map(cached), mapped)
        mapping += element -> mapped /* TODO Use correct position */
        res
      case Delta.Replace(reference, element) =>
        val mapped = f(element)
        val res = Delta.Replace(cached(reference), mapped)
        replace(reference, element -> mapped)
        res
      case Delta.Remove(element) =>
        val res = Delta.Remove(cached(element))
        remove(element)
        res
      case Delta.Clear() =>
        mapping.clear()
        Delta.Clear()
    }

    DeltaBuffer(chgs)
  }

  /** @note `f` must not be side-effecting */
  def mapPure[U](f: T => U): DeltaBuffer[U] = {
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

  def buffer: ReadBuffer[T] = {
    val buf = Buffer[T]()
    buf.changes << changes
    buf
  }

  def mapTo[U](f: T => U): DeltaDict[T, U] = {
    val delta: ReadChannel[Dict.Delta[T, U]] = changes.flatMapSeq {
      case Delta.Insert(position, element) =>
        Seq(Dict.Delta.Insert(element, f(element)))
      case Delta.Replace(reference, element) =>
        Seq(Dict.Delta.Remove(reference),
          Dict.Delta.Insert(element, f(element)))
      case Delta.Remove(element) => Seq(Dict.Delta.Remove(element))
      case Delta.Clear() => Seq(Dict.Delta.Clear())
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
  with reactive.poll.Filter[ReadBuffer, T]
  with reactive.stream.Find[T]
  with reactive.stream.Filter[DeltaBuffer, T, T]
  with reactive.stream.RelativeOrder[T]
  with reactive.stream.Aggregate[ReadBuffer, T]
  with reactive.stream.FilterOrdered[ReadBuffer, T]
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
  def filter(f: T => Boolean): DeltaBuffer[T] =
    DeltaBuffer(changes.partialMap {
      case Delta.Insert(Position.Head(), element) if f(element) =>
        Delta.Insert(Position.Head(), element)

      case Delta.Insert(Position.Last(), element) if f(element) =>
        Delta.Insert(Position.Last(), element)

      case Delta.Insert(Position.Before(reference), element) if f(reference) && f(element) =>
        Delta.Insert(Position.Before(reference), element)

      case Delta.Insert(Position.Before(reference), element) if !f(reference) && f(element) =>
        val insert = elements.drop(indexOf(reference)).find(x => x != element && f(x))
        if (insert.isEmpty) Delta.Insert(Position.Last(), element)
        else Delta.Insert(Position.After(insert.get), element)

      case Delta.Insert(Position.After(reference), element) if f(reference) && f(element) =>
        Delta.Insert(Position.After(reference), element)

      case Delta.Insert(Position.After(reference), element) if !f(reference) && f(element) =>
        val insert = elements.drop(indexOf(reference)).find(x => x != element && f(x))
        if (insert.isEmpty) Delta.Insert(Position.Last(), element)
        else Delta.Insert(Position.Before(insert.get), element)

      case Delta.Replace(reference, element) if f(reference) && f(element) =>
        Delta.Replace(reference, element)

      case Delta.Replace(reference, element) if f(reference) && !f(element) =>
        Delta.Remove(reference)

      case Delta.Replace(reference, element) if !f(reference) && f(element) =>
        val insert = elements.drop(indexOf(reference)).find(x => x != element && f(x))
        if (insert.isEmpty) Delta.Insert(Position.Last(), element)
        else Delta.Insert(Position.Before(insert.get), element)

      case Delta.Remove(element) if f(element) =>
        Delta.Remove(element)

      case Delta.Clear() => Delta.Clear()
    })

  def filter$(f: T => Boolean): ReadBuffer[T] = Buffer(elements.filter(f): _*)
  def distinct$: ReadBuffer[T] = Buffer(elements.distinct: _*)

  def span$(f: T => Boolean): (ReadBuffer[T], ReadBuffer[T]) = {
    val (l, r) = elements.span(f)
    (Buffer(l: _*), Buffer(r: _*))
  }

  def partition$(f: T => Boolean): (ReadBuffer[T], ReadBuffer[T]) = {
    val (l, r) = elements.partition(f)
    (Buffer(l: _*), Buffer(r: _*))
  }

  def find$(f: T => Boolean): Option[T] = elements.find(f)

  def exists$(f: T => Boolean): Boolean = elements.exists(f)
  def forall$(f: T => Boolean): Boolean = elements.forall(f)

  def value(index: Int): T = elements(index)
  def indexOf(handle: T): Int = elements.indexOf(handle)
  def toSeq: ReadChannel[Seq[T]] = changes.map(_ => elements)

  def before(value: T): ReadChannel[T] =
    changes.map(_ => before$(value)).distinct

  def after(value: T): ReadChannel[T] =
    changes.map(_ => after$(value)).distinct

  def beforeOption(value: T): ReadChannel[T] =
    changes.partialMap(Function.unlift(_ => beforeOption$(value)))

  def afterOption(value: T): ReadChannel[T] =
    changes.partialMap(Function.unlift(_ => afterOption$(value)))

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
      case Delta.Insert(Position.Head(), element) => hd := Some(element)
      case Delta.Insert(Position.Last(), element)
        if hd.isEmpty$ => hd := Some(element)
      case Delta.Insert(Position.Before(before), element)
        if hd.contains$(before) => hd := Some(element)
      case Delta.Replace(reference, element)
        if hd.contains$(reference) => hd := Some(element)
      case Delta.Remove(element)
        if hd.contains$(element) => hd := Some(elements.head)
      case _ =>
    }

    hd.values
  }

  def last: ReadChannel[T] = {
    val lst = Opt[T]()

    changes.attach {
      case Delta.Insert(Position.Head(), element)
        if lst.isEmpty$ => lst := Some(element)
      case Delta.Insert(Position.Last(), element) =>
        lst := Some(element)
      case Delta.Insert(Position.After(after), element)
        if lst.contains$(after) => lst := Some(element)
      case Delta.Replace(reference, element)
        if lst.contains$(reference) => lst := Some(element)
      case Delta.Remove(element)
        if lst.contains$(element) => lst := Some(elements.last)
      case _ =>
    }

    lst.values
  }

  def headOption: ReadPartialChannel[T] = {
    val opt = Opt[T]()
    changes.attach(_ =>
      if (opt.get != get.headOption) opt := get.headOption)
    opt
  }

  def lastOption: ReadPartialChannel[T] = {
    val opt = Opt[T]()
    changes.attach(_ =>
      if (opt.get != get.lastOption) opt := get.lastOption)
    opt
  }

  def tail: ReadBuffer[T] = {
    val result = Buffer[T]()
    changes.attach(_ => result.set(get.tail))
    result
  }

  def isHead(element: T): ReadChannel[Boolean] =
    headOption.is(Some(element))

  def isLast(element: T): ReadChannel[Boolean] =
    lastOption.is(Some(element))

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

  def concat(buf: ReadBuffer[T]): ReadBuffer[T] = {
    val res = Buffer[T]()

    changes.merge(buf.changes).attach { _ =>
      res.clear()
      get.foreach(t => res.append(t))
      buf.get.foreach(t => res.append(t))
    }

    res
  }

  def map[U](f: T => U): DeltaBuffer[U]

  def flatMap[U](f: T => ReadBuffer[U]): ReadBuffer[U] =
    Buffer.flatten(map(f).buffer)

  def partialMap[U](f: PartialFunction[T, U]): ReadBuffer[U] =
    flatMap(value => f.lift(value) match {
      case Some(v) => Buffer(v)
      case None => Buffer()
    })

  def flatMapCh[U](f: T => ReadPartialChannel[U]): ReadBuffer[U] =
    flatMap(value => f(value).flatMapBuf {
      case Some(v) => Buffer(v)
      case None => Buffer()
    })

  def flatMapSeq[U](f: T => Seq[U]): ReadBuffer[U] =
    flatMap(value => Buffer.from(f(value)))

  /* Has some conceptual issues, but some ideas may be ported to flatten()
     for better performance.

  def flatMapCh[U](f: T => ReadPartialChannel[U]): ReadBuffer[U] = {
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
        val ch = f(element).values
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
  */

  def foldLeft[U](acc: U)(f: (U, T) => U): ReadChannel[U] =
    changes.map { _ =>
      get.foldLeft(acc)(f)
    }.distinct

  /** Returns first matching row; if it gets deleted, returns next match. */
  def find(f: T => Boolean): ReadPartialChannel[T] = filter(f).buffer.headOption

  def diff(other: ReadBufSet[T]): ReadBuffer[T] = {
    val buf = Buffer[T]()
    val diff = other.elements

    changes.attach { _ =>
      buf.set(elements.diff(diff.toSeq))
    }

    other.changes.attach { _ =>
      buf.set(elements.diff(diff.toSeq))
    }

    buf
  }

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
{
  def update(f: T => T) {
    foreach(t => replace(t, f(t)))
  }
}

case class RefBuf[T]() extends Buffer[Ref[T]] {
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

  def +=(value: T) = append(value)
}
