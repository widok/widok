package org.widok

import scala.collection.mutable

trait ReadMap[A, B] extends Aggregate[(A, B)] {
  import Aggregate.Change

  def currentSize: Int
  def contains(value: (A, B)): Boolean
  def get(key: A): B
  def has(key: A): Boolean
  def apply(key: A) = get(key)

  def valueChanges: ReadChannel[Change[B]] = chChanges.map {
    case Change.Insert(position, element) => Change.Insert(position.map(_._2), element._2)
    case Change.Remove(element) => Change.Remove(element._2)
    case Change.Clear() => Change.Clear()
  }
}

/**
 * A map is an unordered aggregate consisting of (key, value) pairs.
 */
trait UnorderedMap[A, B] extends ReadMap[A, B] {
  private[widok] val mapping = mutable.Map[A, B]()

  def currentSize: Int = mapping.size

  def contains(value: (A, B)) =
    mapping.isDefinedAt(value._1) && mapping(value._1) == value._2

  def get(key: A) = mapping(key)
  def has(key: A) = mapping.contains(key)
}

/**
 * An aggregate map inherits the elements of the referenced aggregate.
 * Changes such as insertions and removals in the aggregate are propagated.
 * A constant value is assigned to each element when it is inserted.
 */
trait AggMap[A, B] extends UnorderedMap[A, B] {
  val parent: Aggregate[A]
  def elementValue: () => B

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
      mapping += element -> elementValue()
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

trait MapCombinators[V, T] { // TODO extends FoldFunctions[T] {
  import Aggregate.Change

  def valueChanges: ReadChannel[Change[T]]

  // def foldLeft[U](acc: U)(f: (U, T) => U): ReadChannel[U] = ???
  // def exists(f: T => Boolean): ReadChannel[Boolean] = ???

  def forall(f: T => ReadChannel[Boolean]): ReadChannel[Boolean] = {
    val falseIds = new mutable.HashMap[T, Unit]()
    val state = LazyVar(falseIds.isEmpty)

    valueChanges.attach {
      case Change.Insert(position, element) =>
        // TODO detach
        f(element).attach { value =>
          if (value) {
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

case class FunctMap[A, B](parent: Aggregate[A], f: A ⇒ B) extends ReadMap[A, B] with MapCombinators[A, B] {
  import Aggregate.Change
  import Aggregate.Position

  private[widok] val chChanges = new RootChannel[Change[(A, B)]] {
    def flush(g: Change[(A, B)] => Unit) {
      parent.foreach { element =>
        g(Change.Insert(Position.Last(), (element, f(element))))
      }
    }
  }

  parent.chChanges.attach {
    case Change.Insert(position, element) =>
      chChanges := Change.Insert(
        position.map(value => (value, f(value))), (element, f(element)))

    case Change.Remove(element) =>
      chChanges := Change.Remove((element, f(element)))

    case Change.Clear() =>
      chChanges := Change.Clear()
  }

  def currentSize: Int = parent.currentSize
  def contains(value: (A, B)): Boolean = has(value._1) && f(value._1) == value._2
  def get(key: A): B = f(key)
  def has(key: A): Boolean = ???
  def foreach(g: ((A, B)) => Unit) {
    parent.foreach(a ⇒ g(a, f(a)))
  }
}

/**
 * A map which uses ``Var`` as a value for each element. This allows for
 * writing capabilities.
 */
case class VarMap[A, B](parent: Aggregate[A], default: B) extends AggMap[A, Var[B]] with MapCombinators[A, Var[B]] {
  def elementValue: () => Var[B] = () => Var(default)
}

case class OptMap[A, B](parent: Aggregate[A]) extends AggMap[A, Opt[B]] with MapCombinators[A, Opt[B]] {
  def elementValue: () => Opt[B] = () => Opt()
}
