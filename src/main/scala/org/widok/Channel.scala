package org.widok

import scala.collection.mutable.ArrayBuffer

import shapeless._

import org.widok.Helpers.Identity

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

case class Channel[T]() extends Identity {
  import Channel.{Observer, Producer}

  private val producers = new ArrayBuffer[Producer[T]]() // TODO Restrict to one?
  private val observers = new ArrayBuffer[Observer[T]]()

  // Finds the first non-empty value a producer returns.
  private def produced: Option[T] =
    producers.foldLeft(Option.empty[T]) { (acc, cur) =>
      acc match {
        case Some(_) => acc
        case _ => cur()
      }
    }

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

  def destroy() {
    producers.clear()
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

  def map[U](f: T => U): Channel[U] = {
    val res = Channel[U]()
    attach(value => res.produce(f(value)))
    res.attach(() => produced.map(f))
    res
  }

  def value[U](f: shapeless.Lens[T, T] => shapeless.Lens[T, U]) =
    lens(f(shapeless.lens[T]))

  /* Two-way lens that propagates back changes. */
  def lens[U](l: shapeless.Lens[T, U]): Channel[U] = {
    val res = Channel[U]()
    var cache: Option[T] = None

    var observer: Observer[T] = null

    val propagateBack = (value: U) =>
      cache.foreach(cur => produce(l.set(cur)(value), observer))

    observer = (value: T) => {
      cache = Some(value)
      res.produce(l.get(value), propagateBack)
    }

    attach(observer)
    res.attach(propagateBack)

    res
  }
}

case class CachedChannel[T](ch: Channel[T] = Channel[T]()) {
  import Channel.{Producer, Observer}

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
  def populate() = ch.populate()
  def attach(observer: Observer[T]) = ch.attach(observer)
  def attach(producer: Producer[T]) = ch.attach(producer)
  def detach(observer: Observer[T]) = ch.detach(observer)
  def detach(producer: Producer[T]) = ch.detach(producer)
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
