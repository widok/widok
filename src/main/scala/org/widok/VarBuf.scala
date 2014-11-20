package org.widok

import scala.collection.mutable

trait ReadVarBuf[T]
  extends ReadBuffer[Var[T]]
  with FilterFunctions[ReadVarBuf, T]
  with MapFunctions[ReadVarBuf, T]
  with FoldFunctions[T]
{
  import Aggregate.Change

  /** Materialise elements to a writable VarBuf. */
  def toVarBuf: VarBuf[T] = VarBuf(this)

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

  def map[U](f: T => U): ReadVarBuf[U] = MappedVarBuf(this, f)
  def partialMap[U](f: PartialFunction[T, U]): ReadVarBuf[U] = ???
  def flatMap[U](f: T => ReadVarBuf[U]): ReadVarBuf[U] = ???
  def takeUntil(ch: ReadChannel[_]): ReadVarBuf[T] = ???
  def equal(value: T): ReadChannel[Boolean] = ???
  def unequal(value: T): ReadChannel[Boolean] = ???

  def foldLeft[U](acc: U)(f: (U, T) => U): ReadChannel[U] = ???

  def exists(f: T => Boolean): ReadChannel[Boolean] = ???

  def forall(f: T => Boolean): ReadChannel[Boolean] = {
    val falseIds = new mutable.HashMap[Var[T], Unit]()
    val state = LazyVar(falseIds.isEmpty)

    chChanges.attach {
      case Change.Insert(position, element) =>
        // TODO detach
        element.attach { value =>
          if (f(value)) {
            if (falseIds.isDefinedAt(element)) {
              falseIds -= element
              state.produce()
            }
          } else {
            falseIds += element -> (())
            state.produce()
          }
        }

      case Change.Remove(element) =>
        if (falseIds.isDefinedAt(element)) {
          falseIds -= element
          state.produce()
        }

      case Change.Clear() =>
        falseIds.clear()
        state.produce()
    }

    state
  }
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
  def apply[T](elements: ReadBuffer[Var[T]]): VarBuf[T] = {
    val buf = VarBuf[T]()
    buf ++= elements
    buf
  }

  def apply[T](elements: T*): VarBuf[T] = {
    val buf = VarBuf[T]()
    buf.set(elements.map(value => Var(value)): _*)
    buf
  }
}

case class FilteredVarBuf[T](parent: ReadVarBuf[T], f: T => Boolean) extends ReadVarBuf[T] with Disposable {
  import Aggregate.Change
  import Aggregate.Position

  private[widok] var mapping = new mutable.HashMap[Var[T], Unit]()

  private[widok] val chChanges = new RootChannel[Change[Var[T]]] {
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
  def get(value: Int): Var[T] = toSeq(value)
  def indexOf(value: Var[T]): Int = toSeq.indexOf(value)
  def toSeq: Seq[Var[T]] = parent.toSeq.filter(mapping.isDefinedAt)

  private[widok] def mapPosition(position: Aggregate.Position[Var[T]]): Aggregate.Position[Var[T]] =
    position match {
      case Position.Head() => Position.Head()
      case Position.Last() => Position.Last()
      case _ =>
        val element = position match {
          case Position.Before(el) => el
          case Position.After(el) => el
        }

        if (mapping.isEmpty) Position.Last()
        else if (mapping.isDefinedAt(element)) position
        else {
          val defined = parent.toSeq.filter(mapping.isDefinedAt)
          val min = defined.minBy(cur => math.abs(parent.indexOf(cur) - parent.indexOf(element)))

          position match {
            case Position.Before(_) => Position.After(min)
            case Position.After(_) => Position.Before(min)
          }
        }
    }

  def parentChange(change: Change[Var[T]]) {
    change match {
      case Change.Insert(position, element) =>
        element.attach { value =>
          if (f(value)) {
            mapping += element -> (())
            chChanges := Change.Insert(mapPosition(position), element)
          } else if (mapping.isDefinedAt(element)) {
            mapping -= element
            chChanges := Change.Remove(element)
          }
        }

      case Change.Remove(element) =>
        // TODO detach above listener here
        if (mapping.isDefinedAt(element)) {
          mapping -= element
          chChanges := Change.Remove(element)
        }

      case Change.Clear() =>
        // TODO detach above listener here
        mapping.clear()
        chChanges := Change.Clear()
    }
  }

  parent.changes.attach(parentChange)

  /** Clear mapping and reapply f() for all parent elements. */
  private[widok] def reset() {
    parentChange(Change.Clear())
    parent.changes.flush(parentChange)
  }

  def dispose() = ???
}

case class MappedVarBuf[T, U](parent: ReadVarBuf[T], f: T => U) extends ReadVarBuf[U] with Disposable {
  import Aggregate.Change
  import Aggregate.Position

  private[widok] var mapping = new mutable.HashMap[Var[T], Var[U]]()

  private[widok] val chChanges = new RootChannel[Change[Var[U]]] {
    def flush(obs: Change[Var[U]] => Unit) {
      parent.foreach { element =>
        obs(Change.Insert(Position.Last(), mapping(element)))
      }
    }
  }

  def currentSize: Int = mapping.size

  def foreach(f: Var[U] => Unit) {
    parent.foreach { element =>
      f(mapping(element))
    }
  }

  def contains(element: Var[U]): Boolean = mapping.values.exists(_ == element)
  def get(value: Int): Var[U] = toSeq(value)
  def indexOf(value: Var[U]): Int = toSeq.indexOf(value)
  def toSeq: Seq[Var[U]] = parent.toSeq.map(mapping)

  def parentChange(change: Change[Var[T]]) {
    change match {
      case Change.Insert(position, element) =>
        /** TODO mapping += element -> element.map(f) */
        mapping += element -> Var(f(element.get))
        mapping(element) << element.map(f)
        chChanges := Change.Insert(position.map(mapping), mapping(element))

      case Change.Remove(element) =>
        val m = mapping(element)
        mapping(element).dispose()
        mapping -= element
        chChanges := Change.Remove(m)

      case Change.Clear() =>
        mapping.foreach(_._2.dispose())
        mapping.clear()
        chChanges := Change.Clear()
    }
  }

  val child = parent.changes.attach(parentChange)

  def dispose() = {
    child.dispose()
    mapping.foreach(_._2.dispose())
  }
}
