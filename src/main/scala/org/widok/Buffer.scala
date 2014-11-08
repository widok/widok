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
  def apply[T]() = new Buffer[T] { }
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

  def splitAt(element: T): (ReadBuffer[T], ReadBuffer[T]) = ???

  def take(count: Int): ReadBuffer[T] = ???
  def skip(count: Int): ReadBuffer[T] = ???

  def headOption: ReadChannel[Option[T]] = ???
  def lastOption: ReadChannel[Option[T]] = ???

  def head: ReadChannel[T] = chChanges.partialMap {
    case Change.Insert(Position.Head(), element) =>
      element

    case Change.Insert(Position.Last(), element)
      if currentSize == 0 => element

    case Change.Insert(Position.Before(before), element)
      if get(0) == before => element
  }

  def last: ReadChannel[T] = lastOption.map(_.get)

  def tail: ReadBuffer[T] = ???

  def isHead(elem: T): ReadChannel[Boolean] = headOption.map(_ == Some(elem))
  def isLast(elem: T): ReadChannel[Boolean] = lastOption.map(_ == Some(elem))

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
    chChanges := Change.Insert(Position.Head(), elem)
    elements.prepend(elem)
  }

  def append(elem: T) {
    chChanges := Change.Insert(Position.Last(), elem)
    elements.append(elem)
  }

  def insertBefore(ref: T, elem: T) {
    chChanges := Change.Insert(Position.Before(ref), elem)
    val position = elements.indexOf(ref) - 1
    elements.insert(position, elem)
  }

  def insertAfter(ref: T, elem: T) {
    chChanges := Change.Insert(Position.After(ref), elem)
    val position = elements.indexOf(ref) + 1
    elements.insert(position, elem)
  }

  def remove(handle: T) {
    chChanges := Change.Remove(handle)
    elements -= handle
  }

  def clear() {
    chChanges := Change.Clear()
    elements.clear()
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
}
