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

/**
 * A map which uses ``Var`` as a value for each element. This allows for
 * writing capabilities.
 */
case class VarMap[A, B](parent: Aggregate[A], default: B) extends AggMap[A, Var[B]] with MapCombinators[A, Var[B]] {
  import Aggregate.Change

  def elementValue: () => Var[B] = () => Var(default)

  def valueChanges: ReadChannel[Change[Var[B]]] =
    chChanges.map {
      case Change.Insert(position, element) => Change.Insert(position.map(_._2), element._2)
      case Change.Remove(element) => Change.Remove(element._2)
      case Change.Clear() => Change.Clear()
    }
}

case class OptMap[A, B](parent: Aggregate[A]) extends AggMap[A, Opt[B]] with MapCombinators[A, Opt[B]] {
  import Aggregate.Change

  def elementValue: () => Opt[B] = () => Opt()

  def valueChanges: ReadChannel[Change[Opt[B]]] =
    chChanges.map {
      case Change.Insert(position, element) => Change.Insert(position.map(_._2), element._2)
      case Change.Remove(element) => Change.Remove(element._2)
      case Change.Clear() => Change.Clear()
    }
}
