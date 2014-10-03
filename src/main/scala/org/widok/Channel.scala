package org.widok

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object Channel {
  type Producer[T] = () => Option[T]
  type Observer[T] = T => Unit

  def unit[T](value: T): Channel[T] = {
    val ch = Channel[T]()
    val producer = () => Some(value)
    ch.attach(producer)
    ch
  }
}

case class Channel[T]() {
  import Channel.{Observer, Producer}

  private val producers = new ArrayBuffer[Producer[T]]() // TODO Restrict to one?
  private val observers = new ArrayBuffer[Observer[T]]()

  override def equals(other: Any) = false

  // Finds the first non-empty value a producer returns.
  private def produced: Option[T] =
    producers.foldLeft(Option.empty[T]) { (acc, cur) =>
      acc match {
        case Some(_) => acc
        case _ => cur()
      }
    }

  def produce(v: T) {
    observers.foreach(_(v))
  }

  def produce(v: T, ignore: Observer[T]*) {
    observers.diff(ignore).foreach(_(v))
  }

  def flatProduce(v: Option[T]) {
    v.foreach(cur => observers.foreach(_(cur)))
  }

  def flatProduce(v: Option[T], ignore: Observer[T]*) {
    v.foreach(cur => observers.diff(ignore).foreach(_(cur)))
  }

  def :=(v: T) = produce(v)

  // Populate observers with a value obtained from a producer.
  def populate() {
    produced.foreach(produce)
  }

  def attach(observer: Observer[T]) {
    assume(!observers.contains(observer))
    observers.append(observer)
  }

  def attach(producer: Producer[T]) {
    assume(!producers.contains(producer))
    producers.append(producer)
  }

  def detach(observer: Observer[T]) = {
    assume(observers.contains(observer))
    observers -= observer
  }

  def detach(producer: Producer[T]) = {
    assume(producers.contains(producer))
    producers -= producer
  }

  def take(count: Int): Channel[T] = {
    assume(count > 0)

    val ch = Channel[T]()
    var taken = 0

    def f: Observer[T] = t =>
      if (taken < count) {
        ch := t
        taken += 1
      } else {
        // TODO Not working?
        // detach(f)
      }

    attach(f)
    ch
  }

  def skip(count: Int): Channel[T] = {
    assume(count > 0)

    val ch = Channel[T]()
    var skipped = 0

    attach(t =>
      if (skipped < count) skipped += 1
      else ch := t)

    ch
  }

  def unique: Channel[T] = {
    val ch = Channel[T]()
    var value: Option[T] = None

    attach(t => if (!value.contains(t)) {
      ch := t
      value = Some(t)
    })

    ch
  }

  def head: Channel[T] = take(1)
  def tail: Channel[T] = skip(1)
  def cache: Cache[T] = Cache(this)

  // Maps each value change of ``other`` to a change of ``this``.
  def zip[U](other: Channel[U]): Channel[(T, U)] = {
    val res = Channel[(T, U)]()
    var tValue = Option.empty[T]

    attach(t => tValue = Some(t))
    other.attach(u => tValue.foreach(t => res.produce((t, u))))

    res
  }

  def map[U](f: T => U): Channel[U] = {
    val res = Channel[U]()
    attach(value => res.produce(f(value)))
    res.attach(() => produced.map(f))
    res
  }

  /* Two-way lens that propagates back changes. */
  def lens[U](get: T => U, set: (T, U) => T): Channel[U] = {
    val res = Channel[U]()
    var cache: Option[T] = None

    var observer: Observer[T] = null

    val propagateBack = (value: U) =>
      cache.foreach(cur => produce(set(cur, value), observer))

    observer = (value: T) => {
      cache = Some(value)
      res.produce(get(value), propagateBack)
    }

    attach(observer)
    res.attach(propagateBack)

    res
  }
}

case class Cache[T](ch: Channel[T]) {
  private var value: Option[T] = None

  ch.attach(t => value = Some(t))

  def channel = ch

  @deprecated("Leads to imperative style", "0.1")
  def get = value

  def update(f: T => T) {
    value.foreach(t => ch := f(t))
  }

  override def toString =
    value.map(_.toString).getOrElse("<undefined>")
}

object Aggregate {
  trait Observer[T] {
    def append(ch: Channel[T])
    def remove(ch: Channel[T])
  }

  // TODO Should be generalised.
  implicit class IntAggregateWithSum(agg: Aggregate[Int]) {
    def sum: Channel[Int] = {
      val ch = Channel[Int]()

      var sum = 0
      val map = new mutable.HashMap[Channel[Int], Int]()

      agg.attach(new Aggregate.Observer[Int] {
        def append(cur: Channel[Int]) {
          cur.attach(value => {
            if (map.get(cur).isDefined) {
              sum -= map(cur)
            }

            map += (cur -> value)
            sum += value

            ch := sum
          })
        }

        def remove(cur: Channel[Int]) {
          sum -= map(cur)
          ch := sum
          map -= cur
        }
      })

      ch
    }
  }
}

// TODO should implement Iterable
case class Aggregate[T]() {
  import Aggregate.Observer

  private val elements = new ArrayBuffer[Channel[T]]()
  private val observers = new ArrayBuffer[Observer[T]]()

  def attach(observer: Observer[T]) {
    assume(!observers.contains(observer))
    observers.append(observer)
  }

  def append(value: Channel[T], ignore: Observer[T]*) {
    elements.append(value)
    observers.diff(ignore).foreach(_.append(elements.last))
  }

  def append(value: T, ignore: Observer[T]*): Channel[T] = {
    append(Channel[T](), ignore: _*)
    elements.last := value
    elements.last
  }

  def contains(value: Channel[T]) =
    elements.contains(value)

  def remove(value: Channel[T], ignore: Observer[T]*) {
    assume(elements.contains(value))
    elements -= value
    observers.diff(ignore).foreach(_.remove(value))
  }

  def clear() {
    elements.foreach(value =>
      observers.foreach(_.remove(value)))
    elements.clear()
  }

  def isEmpty: Channel[Boolean] = {
    val ch = Channel[Boolean]()

    ch.attach(() => Some(elements.isEmpty))

    attach(new Aggregate.Observer[T] {
      def append(cur: Channel[T]) {
        ch := elements.isEmpty
      }

      def remove(cur: Channel[T]) {
        ch := elements.isEmpty
      }
    })

    ch
  }

  def nonEmpty: Channel[Boolean] =
    isEmpty.map(!_)

  def size: Channel[Int] = {
    val ch = Channel[Int]()

    ch.attach(() => Some(elements.size))

    attach(new Aggregate.Observer[T] {
      def append(cur: Channel[T]) {
        ch := elements.size
      }

      def remove(cur: Channel[T]) {
        ch := elements.size
      }
    })

    ch
  }

  def filter(f: T => Boolean): Aggregate[T] = {
    val agg = Aggregate[T]()

    val that = this
    var aggObserver: Aggregate.Observer[T] = null

    val map = new mutable.HashMap[Channel[T], Channel[T]]()

    val observer = new Aggregate.Observer[T] {
      def append(ch: Channel[T]) {
        ch.attach(value => if (f(value)) {
          if (map.contains(ch)) map(ch) := value
          else map += (ch -> agg.append(value, aggObserver))
        } else if (map.contains(ch)) remove(ch))
      }

      def remove(ch: Channel[T]) {
        if (map.contains(ch)) {
          agg.remove(map(ch), aggObserver)
          map -= ch
        }
      }
    }

    // Back-propagate changes.
    aggObserver = new Aggregate.Observer[T] {
      def append(ch: Channel[T]) {
        that.append(ch, observer)
        map += (ch -> ch)
      }

      def remove(ch: Channel[T]) {
        that.remove(ch, observer)
        map -= ch
      }
    }

    attach(observer)

    // TODO Not working properly.
    // agg.attach(aggObserver)

    agg
  }

  def map[U](f: T => U): Aggregate[U] = {
    val agg = Aggregate[U]()

    val map = new mutable.HashMap[Channel[T], Channel[U]]()

    attach(new Aggregate.Observer[T] {
      def append(ch: Channel[T]) {
        ch.attach(value => {
          val target = agg.append(f(value))
          map += (ch -> target)
        })
      }

      def remove(ch: Channel[T]) {
        agg.remove(map(ch))
        map -= ch
      }
    })

    agg
  }

  def forall[U](f: T => Boolean): Channel[Boolean] = {
    val ch = Channel[Boolean]()

    val map = new mutable.HashMap[Channel[T], Boolean]()

    ch.attach(() => Some(true))

    attach(new Aggregate.Observer[T] {
      def append(cur: Channel[T]) {
        cur.attach(value => {
          map += (cur -> f(value))
          ch := map.forall(_._2)
        })
      }

      def remove(cur: Channel[T]) {
        map -= cur
        ch := map.forall(_._2)
      }
    })

    ch
  }

  def cache: CachedAggregate[T] = CachedAggregate(this)
}

// TODO implement
case class CachedAggregate[T](agg: Aggregate[T]) {
  import Aggregate.Observer

  private val values = new mutable.HashMap[Channel[T], Option[T]]()

  agg.attach(new Observer[T] {
    def append(ch: Channel[T]) {

    }

    def remove(ch: Channel[T]) {

    }
  })

  def aggregate = agg

  def filter(f: Channel[T => Boolean]): Aggregate[T] = {
    this.aggregate
  }

  def update(f: T => T): Unit = {

  }
}