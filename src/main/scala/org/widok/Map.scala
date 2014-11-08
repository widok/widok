package org.widok

import scala.collection.mutable

/**
 * A map is an unordered aggregate consisting of (key, value) pairs.
 */
trait UnorderedMap[A, B] extends Aggregate[(A, B)] {
  private[widok] val mapping = mutable.Map[A, B]()

  def currentSize: Int = mapping.size

  def contains(value: (A, B)) =
    mapping.isDefinedAt(value._1) && mapping(value._1) == value._2

  def get(key: A) = mapping(key)
  def has(key: A) = mapping.contains(key)
  def apply(key: A) = get(key)
}

/**
 * An aggregate map inherits the elements of the referenced aggregate.
 * Changes such as insertions and removals in the aggregate are propagated.
 * A constant value is assigned to each element when it is inserted.
 */
case class AggMap[A, B](parent: Aggregate[A], elementValue: B) extends UnorderedMap[A, B] {
  import Aggregate.Change
  import Aggregate.Position

  private[widok] val chChanges = new RootChannel[Change[(A, B)]] {
    def flush(f: Change[(A, B)] => Unit) {
      parent.foreach { element =>
        f(Change.Insert(Position.Last(), (element, mapping(element))))
      }
    }
  }

  def foreach(f: ((A, B)) => Unit) {
    parent.foreach(element => f((element, mapping(element))))
  }

  parent.chChanges.attach {
    case Change.Insert(position, element) =>
      mapping += element -> elementValue
      chChanges := Change.Insert(
        position.map(value => (value, mapping(value))), (element, mapping(element)))

    case Change.Remove(element) =>
      chChanges := Change.Remove((element, mapping(element)))
      mapping -= element

    case Change.Clear() =>
      chChanges := Change.Clear()
      mapping.clear()
  }
}

/**
 * A map which uses ``Var`` as a value for each element. This allows for
 * writing capabilities.
 */
object VarMap {
  def apply[A, B](parent: Aggregate[A], default: B) =
    AggMap[A, Var[B]](parent, Var(default))
}
