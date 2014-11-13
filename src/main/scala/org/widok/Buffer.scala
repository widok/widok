package org.widok

import scala.collection.mutable.ArrayBuffer

/**
 * A buffer is an ordered aggregate with writing capabilities.
 * The values are constant.
 */
trait Buffer[T]
  extends ReadBuffer[T]
  with RootReadBuffer[T]
  with WriteBuffer[T]

object Buffer {
  def apply[T](elements: T*) = {
    val buf = new Buffer[T] { }
    elements.foreach(buf.append)
    buf
  }
}

trait ReadBuffer[T]
  extends Aggregate[T]
  with OrderFunctions[T]
  with BoundedStreamFunctions[ReadBuffer, T]
{
  import Aggregate.Change
  import Aggregate.Position

  def before(value: T): Option[T] = {
    val position = indexOf(value) - 1
    if (position >= 0) Some(get(position))
    else None
  }

  def after(value: T): Option[T] = {
    val position = indexOf(value) + 1
    if (position < currentSize) Some(get(position))
    else None
  }

  def splitAt(element: T): (ReadBuffer[T], ReadBuffer[T]) = {
    val seq = toSeq
    val (left, right) = seq.splitAt(seq.indexOf(element))
    (Buffer(left: _*), Buffer(right: _*))
  }

  def take(count: Int): ReadBuffer[T] = ???
  def skip(count: Int): ReadBuffer[T] = ???

  def headOption: ReadChannel[Option[T]] = chChanges.map { _ =>
    if (currentSize == 0) None
    else Some(get(0))
  }.distinct

  def lastOption: ReadChannel[Option[T]] = chChanges.map { _ =>
    if (currentSize == 0) None
    else Some(get(currentSize - 1))
  }.distinct

  def head: ReadChannel[T] = chChanges.partialMap {
    case Change.Insert(Position.Head(), element) => element
    case Change.Insert(Position.Last(), element) if get(0) == element => element
    case Change.Insert(Position.Before(before), element) if get(0) == element => element
  }

  def last: ReadChannel[T] = chChanges.partialMap {
    case Change.Insert(Position.Head(), element) if get(0) == element => element
    case Change.Insert(Position.Last(), element) => element
    case Change.Insert(Position.After(after), element)
      if get(currentSize - 1) == after => element
  }

  def tail: ReadBuffer[T] = ???

  def isHead(elem: T): ReadChannel[Boolean] = headOption.map(_ == Some(elem))
  def isLast(elem: T): ReadChannel[Boolean] = lastOption.map(_ == Some(elem))

  def insertions: ReadChannel[T] = chChanges.partialMap {
    case Change.Insert(_, element) => element
  }

  def materialise: Seq[T] = {
    import Aggregate._
    val res = ArrayBuffer.empty[T]

    chChanges.attach { // TODO return Resource
      case Change.Insert(Position.Head(), element) => res.prepend(element)
      case Change.Insert(Position.Last(), element) => res += element
      case Change.Insert(Position.Before(before), element) =>
        res.insert(res.indexOf(before), element)
      case Change.Insert(Position.After(after), element) =>
        res.insert(res.indexOf(after) + 1, element)
      case Change.Remove(element) => res -= element
      case Change.Clear() => res.clear()
    }

    res
  }

  override def toString = toSeq.toString()
}

/**
 * A child read buffer will refer to its parent. Some logic needed to
 * be separated from ReadBuffer.
 */
trait RootReadBuffer[T] {
  import Aggregate.Change
  import Aggregate.Position

  private[widok] val elements: ArrayBuffer[T]

  private[widok] val chChanges = new RootChannel[Change[T]] {
    def flush(f: Change[T] => Unit) {
      elements.foreach { element =>
        f(Change.Insert(Position.Last(), element))
      }
    }
  }

  def foreach(f: T => Unit) {
    elements.foreach(f)
  }

  def currentSize: Int = elements.size
  def contains(value: T): Boolean = elements.contains(value)
  def indexOf(value: T): Int = elements.indexOf(value)
  def get(key: Int): T = elements(key)
  def toSeq = elements.toSeq
}

trait WriteBuffer[T] extends UpdateSequenceFunctions[Aggregate, T] {
  import Aggregate.Change
  import Aggregate.Position

  private[widok] val elements = new ArrayBuffer[T]()
  private[widok] val chChanges: Channel[Change[T]]

  def prepend(elem: T) {
    elements.prepend(elem)
    chChanges := Change.Insert(Position.Head(), elem)
  }

  def append(elem: T) {
    elements.append(elem)
    chChanges := Change.Insert(Position.Last(), elem)
  }

  def insertBefore(ref: T, elem: T) {
    val position = elements.indexOf(ref)
    elements.insert(position, elem)
    chChanges := Change.Insert(Position.Before(ref), elem)
  }

  def insertAfter(ref: T, elem: T) {
    val position = elements.indexOf(ref) + 1
    elements.insert(position, elem)
    chChanges := Change.Insert(Position.After(ref), elem)
  }

  def remove(handle: T) {
    elements -= handle
    chChanges := Change.Remove(handle)
  }

  def clear() {
    elements.clear()
    chChanges := Change.Clear()
  }

  def set(items: T*) {
    clear()
    items.foreach(append)
  }

  def appendAll(agg: Aggregate[T]) {
    agg.foreach(append)
  }

  /** Remove all elements from ``agg``. */
  def removeAll(agg: Aggregate[T]) {
    agg.foreach(remove)
  }

  def applyChange(change: Change[T]) {
    change match {
      case Change.Insert(Position.Head(), elem) => prepend(elem)
      case Change.Insert(Position.Last(), elem) => append(elem)
      case Change.Insert(Position.Before(before), elem) => insertBefore(before, elem)
      case Change.Insert(Position.After(after), elem) => insertAfter(after, elem)
      case Change.Remove(elem) => remove(elem)
      case Change.Clear() => clear()
    }
  }
}
