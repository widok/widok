package org.widok

import scala.collection.mutable.ArrayBuffer

import org.widok.Helpers.Identity

object Channel {
  type Observer[T] = T => Unit

  def apply[T]() = new Channel[T] {
    def onAttach(observer: Observer[T]) {}
    def onDetach(observer: Observer[T]) {}
  }

  /**
   * Upon each subscription, emit ``v``. ``v`` is evaluated
   * lazily. Channel.unit() can be considered a cold observable.
   */
  def unit[T](v: => T) = new Channel[T] {
    def onAttach(observer: Observer[T]) { observer(v) }
    def onDetach(observer: Observer[T]) {}
  }

  def from[T](elems: Seq[T]) = new Channel[T] {
    def onAttach(observer: Observer[T]) { elems.foreach(observer) }
    def onDetach(observer: Observer[T]) {}
  }
}

trait Channel[T] extends Identity {
  import Channel.Observer

  private[widok] val observers = new ArrayBuffer[Observer[T]]()

  def cache: CachedChannel[T] = CachedChannel(this)

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

  def onAttach(observer: Observer[T])
  def onDetach(observer: Observer[T])

  def silentAttach(observer: Observer[T]) {
    assume(!observers.contains(observer))
    observers.append(observer)
  }

  def attach(observer: Observer[T]) {
    silentAttach(observer)
    onAttach(observer)
  }

  def silentDetach(observer: Observer[T]) {
    assume(observers.contains(observer))
    observers -= observer
  }

  def detach(observer: Observer[T]) = {
    silentDetach(observer)
    onDetach(observer)
  }

  def destroy() {
    observers.clear()
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

  // Maps each value change of ``other`` to a change of ``this``.
  def zip[U](other: Channel[U]): Channel[(T, U)] = {
    val res = Channel[(T, U)]()
    var tValue = Option.empty[T]

    attach(t => tValue = Some(t))
    other.attach(u => tValue.foreach(t => res.produce((t, u))))

    res
  }

  def create[U](f: T => U): Channel[U] = {
    val that = this

    val res = new Channel[U] {
      def onAttach(observer: Observer[U]) {
        that.onAttach(value => observer(f(value)))
      }

      def onDetach(observer: Observer[U]) {}
    }

    res
  }

  def map[U](f: T => U): Channel[U] = {
    val res = create(f)
    silentAttach(value => res.produce(f(value)))
    res
  }

  def value[U](f: shapeless.Lens[T, T] => shapeless.Lens[T, U]) =
    lens(f(shapeless.lens[T]))

  /* Two-way lens that propagates back changes. */
  def lens[U](l: shapeless.Lens[T, U]): Channel[U] = {
    val res = create(l.get)
    var cache: Option[T] = None

    var observer: Observer[T] = null

    val propagateBack = (value: U) =>
      cache.foreach(cur => produce(l.set(cur)(value), observer))

    observer = (value: T) => {
      cache = Some(value)
      res.produce(l.get(value), propagateBack)
    }

    silentAttach(observer)
    res.attach(propagateBack)

    res
  }
}

case class CachedChannel[T](ch: Channel[T] = Channel[T]()) {
  import Channel.Observer

  private var value: Option[T] = None

  ch.attach(t => value = Some(t))

  @deprecated("Leads to imperative style", "0.1")
  def get = value

  def update(f: T => T) {
    value.foreach(t => ch := f(t))
  }

  override def toString =
    value.map(_.toString).getOrElse("<undefined>")

  /* Inherit all public methods manually from Channel except for cache(). */
  def produce(v: T) = ch.produce(v)
  def produce(v: T, ignore: Observer[T]*) = ch.produce(v, ignore: _*)
  def flatProduce(v: Option[T]) = ch.flatProduce(v)
  def flatProduce(v: Option[T], ignore: Observer[T]*) = ch.flatProduce(v, ignore: _*)
  def :=(v: T) = ch := v
  def attach(observer: Observer[T]) = ch.attach(observer)
  def detach(observer: Observer[T]) = ch.detach(observer)
  def take(count: Int): Channel[T] = ch.take(count)
  def skip(count: Int): Channel[T] = ch.skip(count)
  def unique: Channel[T] = ch.unique
  def head: Channel[T] = ch.head
  def tail: Channel[T] = ch.tail
  def zip[U](other: Channel[U]): Channel[(T, U)] = ch.zip(other)
  def map[U](f: T => U): Channel[U] = ch.map(f)
  def value[U](f: shapeless.Lens[T, T] => shapeless.Lens[T, U]) = ch.value(f)
  def lens[U](l: shapeless.Lens[T, U]): Channel[U] = ch.lens(l)
}
