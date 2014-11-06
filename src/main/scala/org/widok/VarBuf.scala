package org.widok

import scala.collection.mutable

trait ReadVarBuf[T]
  extends ReadBuffer[Var[T]]
  with FilterFunctions[ReadVarBuf, T]
  with MapFunctions[ReadVarBuf, T]
{
  def filter(f: T => Boolean): ReadVarBuf[T] =
    FilteredVarBuf(this, f)

  def filterCh(f: ReadChannel[T => Boolean]): ReadVarBuf[T] = {
    var currentFilter: T => Boolean = null

    val res = FilteredVarBuf(this, (value: T) =>
      if (currentFilter == null) false
      else currentFilter(value))

    f.attach { filter =>
      currentFilter = filter
      res.reset()
    }

    res
  }

  def partition(f: T => Boolean): (ReadVarBuf[T], ReadVarBuf[T]) =
    (filter(f), filter((!(_: Boolean)).compose(f)))

  def distinct: ReadVarBuf[T] = ???

  def span(f: T => Boolean): (ReadVarBuf[T], ReadVarBuf[T]) = {
    /* TODO Must observe buffer */
    val spanned = toSeq.span(cur => f(cur.get))

    val left = VarBuf[T]()
    val right = VarBuf[T]()

    left.set(spanned._1: _*)
    right.set(spanned._2: _*)

    (left, right)
  }

  def map[U](f: T => U): ReadVarBuf[U] = ???
  def partialMap[U](f: PartialFunction[T, U]): ReadVarBuf[U] = ???
  def flatMap[U](f: T => ReadVarBuf[U]): ReadChannel[U] = ???
  def takeUntil(ch: Channel[_]): ReadVarBuf[T] = ???
}

trait WriteVarBuf[T] extends WriteBuffer[Var[T]]

/**
 * Buffer that stores its elements in a ``Var`` container. Therefore,
 * the values can be updated.
 */
case class VarBuf[T]() extends Buffer[Var[T]]
  with ReadVarBuf[T]
  with WriteVarBuf[T]
{
  def prepend(value: T): Var[T] = {
    val elem = Var(value)
    prepend(elem)
    elem
  }

  def append(value: T): Var[T] = {
    val elem = Var(value)
    append(elem)
    elem
  }

  def insertBefore(handle: Var[T], value: T): Var[T] = {
    val elem = Var(value)
    insertBefore(handle, elem)
    elem
  }

  def insertAfter(handle: Var[T], value: T): Var[T] = {
    val elem = Var(value)
    insertAfter(handle, elem)
    elem
  }

  def update(f: T => T) {
    elements.foreach(element => element := f(element.get))
  }

  /** Setter lens which operates on all elements. */
  def setter[V](l: shapeless.Lens[T, T] => shapeless.Lens[T, V]): WriteChannel[V] = {
    val ch = Channel[V]()
    val lens = l(shapeless.lens[T])
    ch.attach(value => update(lens.set(_)(value)))
    ch
  }

  def +=(value: T) = append(value)
}

object VarBuf {
  def unit[T](elements: T*) = {
    val buf = VarBuf[T]()
    buf.set(elements.map(value => Var(value)): _*)
    buf
  }
}

case class FilteredVarBuf[T](parent: ReadVarBuf[T], f: T => Boolean) extends ReadVarBuf[T] {
  import Aggregate.Change
  import Aggregate.Position

  private[widok] var mapping = new mutable.HashMap[Var[T], Unit]()

  private[widok] val chChanges = new Channel[Change[Var[T]]] {
    def dispose() { }
    def request() { }

    def flush(obs: Change[Var[T]] => Unit) {
      parent.foreach { element =>
        if (f(element.get)) obs(Change.Insert(Position.Last(), element))
      }
    }
  }

  def currentSize: Int = mapping.size

  def foreach(f: Var[T] => Unit) {
    parent.foreach { element =>
      if (mapping.isDefinedAt(element)) f(element)
    }
  }

  def contains(element: Var[T]): Boolean = mapping.isDefinedAt(element)
  def get(value: Int): Var[T] =  toSeq(value)
  def indexOf(value: Var[T]): Int = toSeq.indexOf(value)
  def toSeq: Seq[Var[T]] = parent.toSeq.filter(mapping.isDefinedAt)

  val child = parent.changes.attach {
    case Change.Insert(position, element) =>
      element.attach { value =>
        if (f(value)) {
          val mappedPosition = Position.Last[Var[T]]() // TODO must preserve old order via position.map(...)
          chChanges := Change.Insert(mappedPosition, element)
          mapping += element -> (())
        } else if (mapping.isDefinedAt(element)) {
          chChanges := Change.Remove(element)
          mapping -= element
        }
      }

    case Change.Remove(element) =>
      // TODO detach above listener here
      if (mapping.isDefinedAt(element)) {
        chChanges := Change.Remove(element)
        mapping -= element
      }

    case Change.Clear() =>
      clear()
  }

  private[widok] def clear() {
    // TODO detach above listener here
    mapping.clear()
    chChanges := Change.Clear()
  }

  /** Clear mapping and reapply f() for all parent elements. */
  private[widok] def reset() {
    clear()
    child.request()
  }
}