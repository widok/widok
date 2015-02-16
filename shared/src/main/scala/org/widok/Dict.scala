package org.widok

import scala.collection.mutable

/**
 * A dictionary is a reactive ordered map A => B
 */
object Dict {
  sealed trait Delta[A, B]
  object Delta {
    case class Insert[A, B](key: A, value: B) extends Delta[A, B]
    case class Update[A, B](key: A, value: B) extends Delta[A, B]
    case class Remove[A, B](key: A) extends Delta[A, B]
    case class Clear[A, B]() extends Delta[A, B]
  }

  def apply[A, B](): Dict[A, B] = new Dict[A, B] { }

  def apply[A, B](map: Map[A, B]): Dict[A, B] = {
    val dict = Dict[A, B]()
    dict ++= map
    dict
  }
}

object DeltaDict {
  import Dict.Delta

  def apply[A, B](delta: ReadChannel[Delta[A, B]]): DeltaDict[A, B] =
    new DeltaDict[A, B] {
      override val changes = delta
    }
}

trait DeltaDict[A, B]
  extends reactive.stream.Size
  with reactive.stream.Empty
  with reactive.stream.MapDict[DeltaDict, A, B]
  with reactive.stream.Count[B]
  with reactive.stream.Key[A, B]
{
  import Dict.Delta
  val changes: ReadChannel[Delta[A, B]]

  def mapKeys[C](f: A => C): DeltaDict[C, B] =
    DeltaDict[C, B](changes.map {
      case Delta.Insert(k, v) => Delta.Insert(f(k), v)
      case Delta.Update(k, v) => Delta.Update(f(k), v)
      case Delta.Remove(k) => Delta.Remove(f(k))
      case Delta.Clear() => Delta.Clear()
    })

  def mapValues[C](f: B => C): DeltaDict[A, C] =
    DeltaDict[A, C](changes.map {
      case Delta.Insert(k, v) => Delta.Insert(k, f(v))
      case Delta.Update(k, v) => Delta.Update(k, f(v))
      case Delta.Remove(k) => Delta.Remove(k)
      case Delta.Clear() => Delta.Clear()
    })

  def size: ReadChannel[Int] = {
    val count = Var(0)

    changes.attach {
      case Delta.Insert(k, _) => count.update(_ + 1)
      case Delta.Update(k, _) =>
      case Delta.Remove(k) => count.update(_ - 1)
      case Delta.Clear() if count.get != 0 => count := 0
    }

    count
  }

  def isEmpty: ReadChannel[Boolean] = size.equal(0)
  def nonEmpty: ReadChannel[Boolean] = size.unequal(0)

  def exists(f: B => Boolean): ReadChannel[Boolean] = ???
  def count(value: B): ReadChannel[Int] = ???
  def unequal(value: B): ReadChannel[Boolean] = ???
  def contains(value: B): ReadChannel[Boolean] = ???
  def equal(value: B): ReadChannel[Boolean] = ???
  def countUnequal(value: B): ReadChannel[Int] = ???

  def areNot(f: B => Boolean): ReadBufSet[A] = {
    val unequal = BufSet[A]()

    changes.attach {
      case Delta.Insert(k, v) if !f(v) => unequal += k
      case Delta.Update(k, v) =>
        if (!unequal.contains$(k) && !f(v)) unequal += k
        else if (unequal.contains$(k) && f(v)) unequal -= k
      case Delta.Remove(k) if unequal.contains$(k) => unequal -= k
      case Delta.Clear() => unequal.clear()
    }

    unequal
  }

  def forall(f: B => Boolean): ReadChannel[Boolean] = areNot(f).isEmpty

  def value(key: A): ReadPartialChannel[B] = {
    val res = Opt[B]()

    changes.attach {
      case Delta.Insert(`key`, value) => res := value
      case Delta.Update(`key`, value) => res := value
      case Delta.Remove(`key`) => res.clear()
      case Delta.Clear() if res.nonEmpty$ => res.clear()
      case _ =>
    }

    res
  }

  def buffer: Dict[A, B] = {
    val dict = Dict[A, B]()
    dict.changes.subscribe(changes)
    dict
  }
}

trait StateDict[A, B] extends Disposable {
  import Dict.Delta

  private[widok] val mapping = mutable.Map.empty[A, B]

  val changes = new RootChannel[Delta[A, B]] {
    def flush(f: Delta[A, B] => Unit) {
      mapping.foreach { case (key, value) =>
        f(Delta.Insert(key, value))
      }
    }
  }

  private[widok] val subscription = changes.attach {
    case Delta.Insert(key, value) =>
      assert(!mapping.isDefinedAt(key), "Key already exists")
      mapping += key -> value
    case Delta.Update(key, value) => mapping.update(key, value)
    case Delta.Remove(key) => mapping -= key
    case Delta.Clear() => mapping.clear()
  }

  def dispose() {
    subscription.dispose()
  }
}

trait WriteDict[A, B]
  extends reactive.mutate.Dict[A, B]
{
  import Dict.Delta

  val changes: WriteChannel[Delta[A, B]]

  def insert(key: A, value: B) {
    changes := Delta.Insert(key, value)
  }

  def update(key: A, value: B) {
    changes := Delta.Update(key, value)
  }

  def insertAll(map: Map[A, B]) {
    map.foreach(Function.tupled(insert _))
  }

  def remove(key: A) {
    changes := Delta.Remove(key)
  }

  def removeAll(keys: Seq[A]) {
    keys.foreach(remove)
  }

  def clear() {
    changes := Delta.Clear()
  }

  def set(map: Map[A, B]) {
    clear()
    insertAll(map)
  }
}

trait PollDict[A, B]
  extends reactive.poll.Key[A, B]
  with reactive.poll.Empty
  with reactive.poll.FilterMap[ReadDict, A, B]
  with reactive.stream.Key[A, B]
{
  import Dict.Delta

  private[widok] val mapping: mutable.Map[A, B]

  val changes: ReadChannel[Delta[A, B]]

  def keys$: Set[A] = mapping.keySet.toSet
  def values$: Iterable[B] = mapping.values

  def isEmpty$: Boolean = mapping.isEmpty
  def nonEmpty$: Boolean = mapping.nonEmpty

  def value$(key: A): B = mapping(key)

  def get(key: A): Option[B] = mapping.get(key)

  def filter$(f: ((A, B)) => Boolean): ReadDict[A, B] =
    Dict(mapping.filter(f).toMap)

  def toMap: Map[A, B] = mapping.toMap

  def toBuffer: ReadBuffer[(A, B)] = {
    val buf = Buffer[(A, B)]()

    changes.attach { case _ =>
      buf.set(mapping.toSeq)
    }

    buf
  }
}

trait ReadDict[A, B]
  extends PollDict[A, B]
  with DeltaDict[A, B]

trait Dict[A, B]
  extends ReadDict[A, B]
  with WriteDict[A, B]
  with StateDict[A, B]
