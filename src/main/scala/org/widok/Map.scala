package org.widok

import scala.collection.mutable

trait OrderedMap[A, B] extends Aggregate[B] with UnorderedFunctions[A, B]

/**
 * A buffer map inherits the elements from the referenced parent buffer. Thus,
 * changes such as insertions or removals are propagated. The value returned by
 * f() is assigned to each newly inserted element. Elements have the same order
 * as the parent buffer.
 */
trait ChildMap[A, B] extends OrderedMap[A, B] {
  import Aggregate.Change
  import Aggregate.Position

  val parent: ReadBuffer[A]

  private[widok] val mapping = mutable.Map[Ref[A], Ref[B]]()

  val changes = new RootChannel[Change[Ref[B]]] {
    def flush(f: Change[Ref[B]] => Unit) {
      parent.get.foreach { element =>
        if (mapping.isDefinedAt(element))
          f(Change.Insert(Position.Last(), mapping(element)))
      }
    }
  }

  def value(key: Ref[A]): B = mapping(key).get

  def get: Seq[Ref[B]] = parent.get.flatMap(a => mapping.get(a))

  def values: Seq[B] = parent.get.flatMap(a => mapping.get(a).map(_.get))

  def contains(value: B) = ???

  def foreach(f: B => Unit) {
    parent.get.foreach(element =>
      if (mapping.isDefinedAt(element)) f(mapping(element).get))
  }

  def update(key: Ref[A], value: B) {
    assert(mapping.contains(key))

    // TODO Introduce Change.Update
    val before = parent.beforeOption(key)
    changes := Change.Remove(mapping(key))

    val ref = Ref(value)
    mapping += key -> ref

    if (before.isDefined)
      changes := Change.Insert(Position.After(mapping(before.get)), ref)
    else
      changes := Change.Insert(Position.Last(), ref)
  }

  private[widok] def insert(position: Aggregate.Position[Ref[A]], key: Ref[A], value: B) {
    assert(!mapping.contains(key))
    val ref = Ref(value)
    mapping += key -> ref
    changes := Change.Insert(position.map(mapping), ref)
  }

  def remove(key: Ref[A]) {
    val ref = mapping(key)
    mapping -= key
    changes := Change.Remove(ref)
  }

  def clear() {
    mapping.clear()
    changes := Change.Clear()
  }
}

case class BufMap[A, B](parent: ReadBuffer[A], f: A => B)
  extends ChildMap[A, B]
{
  import Aggregate.Change

  parent.changes.attach {
    case Change.Insert(position, element) => insert(position, element, f(element.get))
    case Change.Remove(element) => remove(element)
    case Change.Clear() => clear()
  }
}

case class OptBufMap[A, B](parent: ReadBuffer[A], f: A => ReadChannel[Option[B]])
  extends ChildMap[A, B]
{
  import Aggregate.Change
  import Aggregate.Position

  val attached = mutable.HashMap.empty[Ref[A], ReadChannel[Unit]]

  def valueChange(key: Ref[A], value: Option[B]) {
    if (mapping.contains(key)) {
      if (value.isDefined) update(key, value.get)
      else remove(key)
    } else if (value.isDefined) {
      val ins = parent.get.drop(parent.indexOf(key)).find(mapping.contains)
      if (ins.isEmpty) insert(Position.Last(), key, value.get)
      else insert(Position.Before(ins.get), key, value.get)
    }
  }

  parent.changes.attach {
    case Change.Insert(position, element) =>
      val ch = f(element.get)
      attached += element -> ch.attach(value => valueChange(element, value))

    case Change.Remove(element) =>
      attached(element).dispose()
      attached -= element
      if (mapping.contains(element)) remove(element)

    case Change.Clear() =>
      attached.foreach { case (_, ch) => ch.dispose() }
      attached.clear()
      clear()
  }
}
