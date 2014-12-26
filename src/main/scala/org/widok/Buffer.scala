package org.widok

import scala.collection.mutable

/**
 * A buffer is an ordered aggregate with writing capabilities.
 * The values are constant.
 */
trait Buffer[T] extends ReadBuffer[T] with WriteBuffer[T] {
  import Aggregate.Change
  import Aggregate.Position

  private[widok] val elements: mutable.ArrayBuffer[Ref[T]]

  val changes = new RootChannel[Change[Ref[T]]] {
    def flush(f: Change[Ref[T]] => Unit) {
      elements.foreach { element =>
        f(Change.Insert(Position.Last(), element))
      }
    }
  }
}

object Buffer {
  def apply[T](elements: T*) = {
    val buf = new Buffer[T] { }
    elements.foreach(buf.append)
    buf
  }

  def from[T](elements: Seq[Ref[T]]) = {
    val buf = new Buffer[T] { }
    buf.set(elements)
    buf
  }

  def materialise[T](changes: ReadChannel[Aggregate.Change[Ref[T]]]): ReadBuffer[T] = {
    val buf = Buffer[T]()
    changes.attach(buf.applyChange)
    buf
  }
}

trait ReadBuffer[T]
  extends Aggregate[T]
  with OrderFunctions[T]
  with FilterFunctions[ReadBuffer, T]
  with MapFunctions[ReadBuffer, T]
  with BoundedStreamFunctions[ReadBuffer, T]
{
  import Aggregate.Change
  import Aggregate.Position

  private[widok] val elements: mutable.ArrayBuffer[Ref[T]]

  def mapTo[U](f: T => U): BufMap[T, U] = BufMap(this, f)

  def mapToCh[U](f: T => ReadChannel[Option[U]]): OptBufMap[T, U] =
    OptBufMap(this, f)

  def get: Seq[Ref[T]] = elements

  def value(index: Int): T = elements(index).get

  def values: Seq[T] = elements.map(_.get)

  def foreach(f: T => Unit) {
    elements.foreach(element => f(element.get))
  }

  def contains(value: T): ReadChannel[Boolean] =
    changes.map(_ => elements.contains(value)).distinct

  def indexOf(handle: Ref[T]): Int = elements.indexOf(handle)

  def toSeq: ReadChannel[Seq[T]] =
    changes.map(_ => elements.map(_.get))

  def before(value: Ref[T]): Ref[T] = {
    val position = indexOf(value) - 1
    elements(position)
  }

  def beforeOption(value: Ref[T]): Option[Ref[T]] = {
    val position = indexOf(value) - 1
    if (position >= 0) Some(elements(position))
    else None
  }

  def after(value: Ref[T]): Ref[T] = {
    val position = indexOf(value) + 1
    elements(position)
  }

  def afterOption(value: Ref[T]): Option[Ref[T]] = {
    val position = indexOf(value) + 1
    if (position < get.size) Some(elements(position))
    else None
  }

  def splitAt(element: Ref[T]): (ReadBuffer[T], ReadBuffer[T]) = {
    val (left, right) = get.splitAt(elements.indexOf(element))
    (Buffer.from(left), Buffer.from(right))
  }

  def take(count: Int): ReadBuffer[Ref[T]] = ???

  def skip(count: Int): ReadBuffer[Ref[T]] = ???

  def headOption: ReadChannel[Option[Ref[T]]] = changes.map { _ =>
    if (get.isEmpty) None
    else Some(get.head)
  }.distinct

  def lastOption: ReadChannel[Option[Ref[T]]] = changes.map { _ =>
    if (get.isEmpty) None
    else Some(get.last)
  }.distinct

  def head: ReadChannel[Ref[T]] = changes.partialMap {
    case Change.Insert(Position.Head(), element) => element
    case Change.Insert(Position.Last(), element) if get.head == element => element
    case Change.Insert(Position.Before(before), element) if get.head == element => element
  }.distinct

  def last: ReadChannel[Ref[T]] = changes.partialMap {
    case Change.Insert(Position.Head(), element) if get.head == element => element
    case Change.Insert(Position.Last(), element) => element
    case Change.Insert(Position.After(after), element)
      if get.last == after => element
  }.distinct

  def tail: ReadBuffer[T] = ???

  def isHead(element: Ref[T]): ReadChannel[Boolean] = headOption.map(_ == Some(element))

  def isLast(element: Ref[T]): ReadChannel[Boolean] = lastOption.map(_ == Some(element))

  def filter(f: T => Boolean): ReadBuffer[T] = {
    val buf = Buffer[T]()
    val filtered = new mutable.HashSet[Ref[T]]()

    changes.attach {
      case Change.Insert(Position.Head(), element) =>
        if (f(element.get)) {
          filtered.add(element)
          buf.prepend(element)
        }

      case Change.Insert(Position.Last(), element) =>
        if (f(element.get)) {
          filtered.add(element)
          buf.append(element)
        }

      case Change.Insert(Position.Before(reference), element) =>
        if (f(element.get)) {
          if (filtered.contains(reference)) buf.insertBefore(reference, element)
          else {
            val insert = get.drop(indexOf(reference)).find(filtered.contains)
            if (insert.isEmpty) buf.append(element)
            else buf.insertAfter(insert.get, element)
          }

          filtered.add(element)
        }

      case Change.Insert(Position.After(reference), element) =>
        if (f(element.get)) {
          if (filtered.contains(reference)) buf.insertAfter(reference, element)
          else {
            val insert = get.drop(indexOf(reference)).find(filtered.contains)
            if (insert.isEmpty) buf.append(element)
            else buf.insertBefore(insert.get, element)
          }

          filtered.add(element)
        }

      case Change.Remove(element) =>
        if (filtered.contains(element)) {
          filtered -= element
          buf.remove(element)
        }

      case Change.Clear() =>
        filtered.clear()
        buf.clear()
    }

    buf
  }

  def partition(f: T => Boolean): (ReadBuffer[T], ReadBuffer[T]) =
    (filter(f), filter((!(_: Boolean)).compose(f)))

  def distinct: ReadBuffer[T] = ???

  def span(f: T => Boolean): (ReadBuffer[T], ReadBuffer[T]) = {
    val left = Buffer[T]()
    val right = Buffer[T]()

    changes.attach { _ =>
      val (leftSpan, rightSpan) = get.span(handle => f(handle.get))

      left.set(leftSpan)
      left.set(rightSpan)
    }

    (left, right)
  }

  def view[U](lens: T => ReadChannel[U]): ReadBuffer[T] = {
    flatMapCh { t =>
      lens(t.get).map(x => Some(t))
    }
  }

  def map[U](f: T => U): ReadBuffer[U] = {
    val buf = Buffer[U]()
    val mapping = new mutable.HashMap[Ref[T], Ref[U]]()

    changes.attach {
      case Change.Insert(Position.Head(), element) =>
        mapping += (element -> buf.prepend(f(element.get)))
      case Change.Insert(Position.Last(), element) =>
        mapping += (element -> buf.append(f(element.get)))
      case Change.Insert(Position.Before(handle), element) =>
        mapping += (before(handle) -> buf.insertBefore(mapping(handle), f(element.get)))
      case Change.Insert(Position.After(handle), element) =>
        mapping += (after(handle) -> buf.insertAfter(mapping(handle), f(element.get)))
      case Change.Remove(element) =>
        buf.remove(mapping(element))
        mapping -= element
      case Change.Clear() =>
        buf.clear()
        mapping.clear()
    }

    buf
  }

  def partialMap[U](f: PartialFunction[T, U]): ReadBuffer[U] = ???

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

  def flatMapCh[U](f: Ref[T] => ReadChannel[Option[Ref[U]]]): ReadBuffer[U] = {
    val res = Buffer[U]()
    val values = mutable.HashMap.empty[Ref[T], Option[Ref[U]]]
    val attached = mutable.HashMap.empty[Ref[T], ReadChannel[Unit]]

    def rerender() {
      res.clear()
      elements.foreach { handle =>
        if (values.isDefinedAt(handle) && values(handle).isDefined)
          res.append(values(handle).get)
      }
    }

    def valueChange(handle: Ref[T], value: Option[Ref[U]]) {
      // TODO Use Change.Update
      values += handle -> value
      rerender()
    }

    changes.attach {
      case Change.Insert(position, element) =>
        values += element -> None
        val ch = f(element)
        attached += element -> ch.attach(value => valueChange(element, value))

      case Change.Remove(element) =>
        attached(element).dispose()
        attached -= element
        values -= element
        rerender()

      case Change.Clear() =>
        attached.foreach { case (_, ch) => ch.dispose() }
        attached.clear()
        values.clear()
        rerender()
    }

    res
  }

  def takeUntil(ch: ReadChannel[_]): ReadBuffer[T] = ???

  def equal(value: T): ReadChannel[Boolean] = ???

  def unequal(value: T): ReadChannel[Boolean] = ???

  def insertions: ReadChannel[T] = changes.partialMap {
    case Change.Insert(_, element) => element.get
  }

  override def toString = get.toString()
}

trait WriteBuffer[T] extends UpdateSequenceFunctions[ReadBuffer, T] {
  import Aggregate.Change
  import Aggregate.Position

  private[widok] val elements = new mutable.ArrayBuffer[Ref[T]]()
  val changes: Channel[Change[Ref[T]]]

  def prepend(element: T): Ref[T] = {
    val handle = Ref[T](element)
    elements.prepend(handle)
    changes := Change.Insert(Position.Head(), handle)
    handle
  }

  def prepend(element: Ref[T]) {
    elements.prepend(element)
    changes := Change.Insert(Position.Head(), element)
  }

  def append(element: T): Ref[T] = {
    val handle = Ref[T](element)
    elements.append(handle)
    changes := Change.Insert(Position.Last(), handle)
    handle
  }

  def append(element: Ref[T]) {
    elements.append(element)
    changes := Change.Insert(Position.Last(), element)
  }

  def insertBefore(reference: Ref[T], element: T): Ref[T] = {
    val position = elements.indexOf(reference)
    val handle = Ref[T](element)
    elements.insert(position, handle)
    changes := Change.Insert(Position.Before(reference), handle)
    handle
  }

  def insertBefore(reference: Ref[T], element: Ref[T]) {
    val position = elements.indexOf(reference)
    elements.insert(position, element)
    changes := Change.Insert(Position.Before(reference), element)
  }

  def insertAfter(reference: Ref[T], element: T): Ref[T] = {
    val position = elements.indexOf(reference) + 1
    val handle = Ref[T](element)
    elements.insert(position, handle)
    changes := Change.Insert(Position.After(reference), handle)
    handle
  }

  def insertAfter(reference: Ref[T], element: Ref[T]) {
    val position = elements.indexOf(reference) + 1
    elements.insert(position, element)
    changes := Change.Insert(Position.After(reference), element)
  }

  def remove(element: Ref[T]) {
    val position = elements.indexOf(element)
    elements.remove(position)
    changes := Change.Remove(element)
  }

  def clear() {
    elements.clear()
    changes := Change.Clear()
  }

  def set(elements: Seq[Ref[T]]) {
    clear()
    elements.foreach(append)
  }

  def appendAll(buf: ReadBuffer[T]) {
    buf.foreach(t => append(t))
  }

  /** Remove all elements that ``buf`` contains. */
  def removeAll(buf: ReadBuffer[T]) {
    buf.get.foreach(t => remove(t))
  }

  def applyChange(change: Change[Ref[T]]) {
    change match {
      case Change.Insert(Position.Head(), element) => prepend(element)
      case Change.Insert(Position.Last(), element) => append(element)
      case Change.Insert(Position.Before(reference), element) => insertBefore(reference, element)
      case Change.Insert(Position.After(reference), element) => insertAfter(reference, element)
      case Change.Remove(element) => remove(element)
      case Change.Clear() => clear()
    }
  }
}
