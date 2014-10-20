package org.widok

import scala.collection.mutable.ArrayBuffer

import org.widok.Helpers.Identity

object Channel {
  type Observer[T] = T => Unit

  def apply[T]() = new Channel[T] {
    def flush(observer: Observer[T]) { }
  }

  /**
   * Upon each subscription, emit ``v``. ``v`` is evaluated
   * lazily. Channel.unit() can be considered a cold observable.
   */
  def unit[T](v: => T) = StateChannel(v)

  def sync[T](ch: Channel[T], ch2: Channel[T]) {
    var obs: Observer[T] = null
    var obs2: Observer[T] = null

    obs = value => ch2.produce(value, obs2)
    obs2 = value => ch.produce(value, obs)

    ch.attach(obs)
    ch2.attach(obs2)
  }
}

trait Channel[T] extends Identity {
  import Channel.Observer

  private[widok] val observers = new ArrayBuffer[Observer[T]]()

  def flush(observer: Observer[T])

  def cache: CachedChannel[T] = {
    val res = CachedChannel[T]()
    Channel.sync(this, res)
    res
  }

  def attach(observer: Observer[T]) {
    assume(!observers.contains(observer))
    observers.append(observer)
    flush(observer)
  }

  def attachFirst(observer: Observer[T]) {
    assume(!observers.contains(observer))
    observers.prepend(observer)
    flush(observer)
  }

  def detach(observer: Observer[T]) = {
    assume(observers.contains(observer))
    observers -= observer
  }

  def destroy() {
    observers.clear()
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
  def head: Channel[T] = take(1)
  def tail: Channel[T] = skip(1)

  def map[U](f: T => U): ChildChannel[T, U] = {
    val res = ChildChannel(this, f)
    attach(value => res := f(value))
    res
  }

  def take(count: Int): ChildChannel[T, T] = {
    assume(count > 0)

    var taken = 1
    val ch = ChildChannel(this, identity[T])
    var obs: Observer[T] = null

    obs = t => {
      ch := t

      if (taken < count) taken += 1
      else detach(obs)
    }

    attach(obs)
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
}

object StateChannel {
  import Channel.Observer

  def apply[T](v: => T) = new Channel[T] {
    def flush(observer: Observer[T]) { observer(v) }
    def produce() { observers.foreach(_(v)) }
  }
}

case class ChildChannel[T, U](parent: Channel[T], f: T => U) extends Channel[U] {
  import Channel.Observer

  def flush(observer: Observer[U]) { parent.flush(observer.compose(f)) }
}

case class CachedChannel[T]() extends Channel[T] {
  import Channel.Observer

  protected var cached: Option[T] = None
  attach(t => cached = Some(t))

  def flush(observer: Observer[T]) {
    cached.foreach(t => observer(t))
  }

  @deprecated("Leads to imperative style", "0.1")
  def get = cached

  def update(f: T => T) {
    cached.foreach(t => this := f(t))
  }

  def unique: Channel[T] = {
    val ch = ChildChannel(this, identity[T])
    attachFirst(t => if (!cached.contains(t)) ch := t)
    ch
  }

  // Maps each value change of ``other`` to a change of ``this``.
  def zip[U](other: Channel[U]): Channel[(T, U)] = {
    val res = Channel[(T, U)]()
    other.attach(u => cached.foreach(t => res := (t, u)))
    res
  }

  def value[U](f: shapeless.Lens[T, T] => shapeless.Lens[T, U]) =
    lens(f(shapeless.lens[T]))

  /* Two-way lens that propagates back changes to all observers. */
  def lens[U](l: shapeless.Lens[T, U]): Channel[U] = {
    val res = ChildChannel[T, U](this, l.get)

    var observer: Observer[T] = null

    val propagateBack = (value: U) =>
      cached.foreach(cur => produce(l.set(cur)(value), observer))

    observer = value => res.produce(l.get(value), propagateBack)

    attach(observer)
    res.attach(propagateBack)

    res
  }

  override def toString =
    cached.map(_.toString).getOrElse("<undefined>")
}