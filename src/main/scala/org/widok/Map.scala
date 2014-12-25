package org.widok

import scala.collection.mutable

trait UnorderedMap[A, B] extends Aggregate[B] with UnorderedFunctions[A, B]

/**
 * A buffer map inherits the elements from the referenced parent buffer. Thus,
 * changes such as insertions or removals are propagated. The value returned by
 * f() is assigned to each newly inserted element.
 */
case class BufMap[A, B](parent: ReadBuffer[A], f: A => B)
  extends UnorderedMap[A, B]
{
  import Aggregate.Change
  import Aggregate.Position

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

  def get: Seq[Ref[B]] = parent.get.map(handle => mapping(handle))

  def values: Seq[B] = parent.get.map(a => mapping(a).get)

  def contains(value: B) = ???

  def foreach(f: B => Unit) {
    parent.get.foreach(element => f(mapping(element).get))
  }

  def remove(key: Ref[A]) {
    mapping -= key
  }

  def update(key: Ref[A], value: B) {
    assert(mapping.contains(key))

    val handle = Ref(value)

    // TODO Introduce Change.Update
    val before = parent.beforeOption(key)
    changes := Change.Remove(mapping(key))
    if (before.isDefined)
      changes := Change.Insert(Position.After(mapping(before.get)), handle)
    else
      changes := Change.Insert(Position.Last(), handle)

    mapping += key -> handle
  }

  parent.changes.attach {
    case Change.Insert(position, element) =>
      mapping += element -> Ref(f(element.get))
      changes := Change.Insert(position.map(mapping), mapping(element))

    case Change.Remove(element) =>
      val v = mapping(element)
      mapping -= element
      changes := Change.Remove(v)

    case Change.Clear() =>
      mapping.clear()
      changes := Change.Clear()
  }
}