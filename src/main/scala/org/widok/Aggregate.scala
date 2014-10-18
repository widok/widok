package org.widok

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

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

      agg.attach(new Observer[Int] {
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

  // Implemented as a class in order to prevent code duplication.
  private[widok] class FilterImpl[T](parent: Aggregate[T]) {
    var f: T => Boolean = null
    val result = Aggregate[T]()
    val map = mutable.HashMap[Channel[T], Channel[T]]()
    var resultObserver: Observer[T] = null

    private val observers = mutable.HashMap[Channel[T], Channel.Observer[T]]()

    def add(parent: Channel[T], value: T) {
      val ch = Channel[T]()
      map += ((parent, ch))

      ch.attach(value => {
        parent.detach(observers(parent)) // TODO Find a better way.
        parent.produce(value)
        parent.attach(observers(parent))

        if (!f(value)) delete(parent)
      })

      result.append(ch, resultObserver)
      ch := value
    }

    def delete(ch: Channel[T]) {
      if (map.contains(ch)) {
        result.remove(map(ch), resultObserver)
        map -= ch
      }
    }

    val observer = new Observer[T] {
      def append(ch: Channel[T]) {
        val observer: Channel.Observer[T] = value =>
          if (f != null && f(value)) {
            if (map.contains(ch)) map(ch) := value
            else add(ch, value)
          } else delete(ch)

        observers += (ch -> observer)
        ch.attach(observer)
      }

      def remove(ch: Channel[T]) {
        delete(ch)
        observers -= ch
      }
    }

    // Back-propagate changes.
    resultObserver = new Observer[T] {
      def append(ch: Channel[T]) {
        parent.append(ch, observer)
        map += (ch -> ch)
      }

      def remove(ch: Channel[T]) {
        val key = map.find(_._2 == ch).get._1
        parent.remove(key, observer)
        map -= key
      }
    }

    parent.attach(observer)
    result.attach(resultObserver)
  }
}

// TODO should implement Iterable
case class Aggregate[T]() {
  import Aggregate.Observer

  private[widok] val elements = new ArrayBuffer[Channel[T]]()
  private val observers = new ArrayBuffer[Observer[T]]()

  def cache: CachedAggregate[T] = CachedAggregate(this)

  def attach(observer: Observer[T]) {
    assume(!observers.contains(observer))
    observers.append(observer)
  }

  def append(value: Channel[T], ignore: Observer[T]*) {
    elements.append(value)
    observers.diff(ignore).foreach(_.append(elements.last))
  }

  def append(ignore: Observer[T]*): Channel[T] = {
    append(Channel[T](), ignore: _*)
    elements.last
  }

  def detach(observer: Observer[T]) {
    assume(observers.contains(observer))
    observers -= observer
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

  def clear(ignore: Observer[T]*) {
    while (elements.nonEmpty) {
      val head = elements.head
      elements -= head
      observers.diff(ignore).foreach(_.remove(head))
    }
  }

  def isEmpty: Channel[Boolean] = {
    val ch = Channel.unit(elements.isEmpty)

    attach(new Aggregate.Observer[T] {
      def append(cur: Channel[T]) { ch.produce() }
      def remove(cur: Channel[T]) { ch.produce() }
    })

    ch
  }

  def nonEmpty: Channel[Boolean] =
    isEmpty.map(!_)

  def size: Channel[Int] = {
    val ch = Channel.unit(elements.size)

    attach(new Aggregate.Observer[T] {
      def append(cur: Channel[T]) { ch.produce() }
      def remove(cur: Channel[T]) { ch.produce() }
    })

    ch
  }

  def filter(f: T => Boolean): Aggregate[T] = {
    val impl = new Aggregate.FilterImpl(this)
    impl.f = f
    impl.result
  }

  def map[U](f: T => U): Aggregate[U] = {
    val agg = Aggregate[U]()

    val map = new mutable.HashMap[Channel[T], Channel[U]]()

    attach(new Aggregate.Observer[T] {
      def append(ch: Channel[T]) {
        val target = agg.append()

        ch.attach(value => {
          target := f(value)
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
    val map = new mutable.HashMap[Channel[T], Boolean]()
    val ch = Channel.unit(map.forall(_._2))

    attach(new Aggregate.Observer[T] {
      def append(cur: Channel[T]) {
        cur.attach(value => {
          map += (cur -> f(value))
          ch.produce()
        })
      }

      def remove(cur: Channel[T]) {
        map -= cur
        ch.produce()
      }
    })

    ch
  }
}

case class CachedAggregate[T](agg: Aggregate[T] = Aggregate[T]()) {
  import Aggregate.Observer

  private val values = new mutable.HashMap[Channel[T], T]()

  agg.attach(new Observer[T] {
    def append(ch: Channel[T]) {
      ch.attach(cur => values += (ch -> cur))
    }

    def remove(ch: Channel[T]) {
      values -= ch
    }
  })

  /* Return immutable version of the value map. */
  @deprecated("Leads to imperative style", "0.1")
  def get = values.toMap

  def flush() {
    agg.elements.foreach(ch =>
      if (values.contains(ch)) ch := values(ch))
  }

  def filterCh(f: Channel[T => Boolean]): Aggregate[T] = {
    val impl = new Aggregate.FilterImpl(agg)

    f.attach(filter => {
      impl.f = filter

      impl.result.clear(impl.resultObserver)
      impl.map.clear() // TODO This should not be necessary.
      assume(impl.map.size == 0)

      agg.elements.foreach { ch =>
        if (values.contains(ch) && filter(values(ch)))
          impl.add(ch, values(ch))
      }
    })

    impl.result
  }

  def update(f: T => T) {
    agg.elements.foreach { ch =>
      if (values.contains(ch)) {
        val value = f(values(ch))
        values += (ch -> value)
        ch := value
      }
    }
  }

  /* Inherit all public methods manually from Aggregate except for cache(). */
  def attach(observer: Observer[T]) = agg.attach(observer)
  def append(value: Channel[T], ignore: Observer[T]*) = agg.append(value, ignore: _*)
  def append(ignore: Observer[T]*): Channel[T] = agg.append(ignore: _*)
  def detach(observer: Observer[T]) = agg.detach(observer)
  def append(value: T, ignore: Observer[T]*): Channel[T] = agg.append(value, ignore: _*)
  def contains(value: Channel[T]) = agg.contains(value)
  def remove(value: Channel[T], ignore: Observer[T]*) = agg.remove(value, ignore: _*)
  def clear(ignore: Observer[T]*) = agg.clear(ignore: _*)
  def isEmpty: Channel[Boolean] = agg.isEmpty
  def nonEmpty: Channel[Boolean] = agg.nonEmpty
  def size: Channel[Int] = agg.size
  def filter(f: T => Boolean): Aggregate[T] = agg.filter(f)
  def map[U](f: T => U): Aggregate[U] = agg.map(f)
  def forall[U](f: T => Boolean): Channel[Boolean] = agg.forall(f)
}