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
{
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

  def toVarMap[U](default: U): AggMap[T, Var[U]] = VarMap(this, default)

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

  private[widok] val chChanges = new Channel[Change[T]] {
    def request() { }
    def dispose() { }
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
    val position = elements.indexOf(ref) - 1
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
}
