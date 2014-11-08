package org.widok

import scala.collection.mutable

import org.widok.Helpers.Identity

object Channel {
  type Observer[T, U] = T => Result[U]

  def apply[T](): Channel[T] =
    new RootChannel[T] {
      def flush(f: T => Unit) { }
    }
}

trait Result[T] {
  def valueOpt: Option[T]
}

object Result {
  case class Next[T](value: Option[T]) extends Result[T] { def valueOpt = value }
  case class Done[T](value: Option[T]) extends Result[T] { def valueOpt = value }
}

trait ReadChannel[T]
  extends Identity
  with StreamFunctions[ReadChannel, T]
  with FilterFunctions[ReadChannel, T]
  with FoldFunctions[T]
  with MapFunctions[ReadChannel, T]
  with IterateFunctions[T]
  with SizeFunctions[T]
  with Disposable
{
  import Channel.Observer

  private[widok] val children = mutable.Queue[ChildChannel[T, Any]]()

  /** Flush data and call f for each element */
  def flush(f: T => Unit)

  def attach(f: T => Unit): ReadChannel[Unit] =
    forkUni { value =>
      f(value)
      Result.Next(None)
    }

  def silentAttach(f: T => Unit): ReadChannel[Unit] =
    forkUni(value => {
      f(value)
      Result.Next(None)
    }, silent = true)

  def buffer: VarBuf[T] = {
    val buf = VarBuf[T]()
    attach(buf += Var(_))
    buf
  }

  /** Uni-directional read/write fork */
  def forkUni[U](observer: Observer[T, U], silent: Boolean = false): ReadChannel[U] = {
    val ch = UniChildChannel[T, U](this, observer)
    children += ch.asInstanceOf[ChildChannel[T, Any]] // TODO Get rid of cast
    if (!silent) flush(ch.process)
    ch
  }

  /** Uni-directional flat fork */
  def forkFlat[U](observer: Observer[T, ReadChannel[U]], silent: Boolean = false): ReadChannel[U] = {
    val ch = FlatChildChannel[T, U](this, observer)
    children += ch.asInstanceOf[ChildChannel[T, Any]] // TODO Get rid of cast
    if (!silent) flush(ch.process)
    ch
  }

  def filter(f: T => Boolean): ReadChannel[T] =
    forkUni { value =>
      if (f(value)) Result.Next(Some(value))
      else Result.Next(None)
    }

  def filterCh(f: ReadChannel[T => Boolean]): ReadChannel[T] = ???

  def partition(f: T => Boolean): (ReadChannel[T], ReadChannel[T]) =
    (filter(f), filter((!(_: Boolean)).compose(f)))

  def take(count: Int): ReadChannel[T] = {
    assert(count > 0)
    var cnt = count
    forkUni { value =>
      if (cnt > 1) { cnt -= 1; Result.Next(Some(value)) }
      else Result.Done(Some(value))
    }
  }

  def skip(count: Int): ReadChannel[T] = {
    assert(count > 0)
    var cnt = count
    forkUni { value =>
      if (cnt > 0) { cnt -= 1; Result.Next(None) }

      // TODO Create a new result type which continues processing
      // the stream without calling this callback.
      else Result.Next(Some(value))
    }
  }

  def head: ReadChannel[T] = forkUni(value => Result.Done(Some(value)))
  def tail: ReadChannel[T] = skip(1)

  def isHead(value: T): ReadChannel[Boolean] =
    take(1).map(_ == value)

  def span(f: T => Boolean): (ReadChannel[T], ReadChannel[T]) = ???

  def map[U](f: T => U): ReadChannel[U] =
    forkUni { value =>
      Result.Next(Some(f(value)))
    }

  def foreach(f: T => Unit): ReadChannel[Unit] =
    forkUni { value =>
      Result.Next(Some(f(value)))
    }

  def equal(value: T): ReadChannel[Boolean] =
    forkUni { t =>
      Result.Next(Some(t == value))
    }

  def unequal(value: T): ReadChannel[Boolean] =
    forkUni { t =>
      Result.Next(Some(t != value))
    }

  def flatMap[U](f: T => ReadChannel[U]): ReadChannel[U] =
    forkFlat(value => Result.Next(Some(f(value))))

  // TODO Not covered by flatMap() in ``MapFunctions``. Probably ``Channel`` needs to
  // be co-variant.
  def flatMapCh[U](f: T => Channel[U]): Channel[U] = ???

  def partialMap[U](f: PartialFunction[T, U]): ReadChannel[U] =
    forkUni { value =>
      Result.Next(f.lift(value))
    }

  /** @note Caches the accumulator value. */
  def foldLeft[U](acc: U)(f: (U, T) => U): ReadChannel[U] = {
    var accum = acc
    forkFlat { value =>
      accum = f(accum, value)
      Result.Next(Some(Var(accum)))
    }
  }

  def exists(f: T => Boolean): ReadChannel[Boolean] = {
    forkUni { value =>
      if (f(value)) Result.Done(Some(true))
      else Result.Next(None)
    }
  }

  def forall(f: T => Boolean): ReadChannel[Boolean] = ???

  def contains(needle: T): ReadChannel[Boolean] =
    forkUni { value =>
      if (value == needle) Result.Done(Some(true))
      else Result.Next(None)
    }

  def takeUntil(ch: ReadChannel[_]): ReadChannel[T] = {
    val res = forkUni { value =>
      Result.Next(Some(value))
    }

    ch.forkUni[Any] { _ =>
      res.dispose()
      Result.Done(None)
    }

    res
  }

  def writeTo(write: WriteChannel[T]): Channel[T] = {
    assert(this != write)
    val res = Channel[T]()
    write << res
    res << this
    res
  }

  def distinct: ReadChannel[T] = {
    var cur = Option.empty[T]
    forkUni { value =>
      if (cur.contains(value)) Result.Next(None)
      else {
        cur = Some(value)
        Result.Next(cur)
      }
    }
  }

  def attached: Boolean
}

trait WriteChannel[T] {
  import Channel.Observer

  private[widok] val children: mutable.Queue[ChildChannel[T, Any]]

  def flush(f: T => Unit)

  def produce(value: T) {
    children.dequeueAll { child =>
      if (!child.disposable) child.process(value)
      child.disposable
    }
  }

  def produce[U](value: T, ignore: ReadChannel[U]*) {
    assume(ignore.forall(children.contains))
    children.diff(ignore).dequeueAll { child =>
      if (!child.disposable) child.process(value)
      child.disposable
    }
  }

  def flatProduce(value: Option[T]) {
    value.foreach { v =>
      children.dequeueAll { child =>
        if (!child.disposable) child.process(v)
        child.disposable
      }
    }
  }

  def flatProduce[U](value: Option[T], ignore: Observer[T, U]*) {
    assume(ignore.forall(children.contains))
    value.foreach { v =>
      children.diff(ignore).dequeueAll { child =>
        if (!child.disposable) child.process(v)
        child.disposable
      }
    }
  }

  /** Bi-directional fork */
  def forkBi[U](fwd: Observer[T, U], bwd: Observer[U, T], silent: Boolean = false): Channel[U] = {
    val ch = BiChildChannel[T, U](this, fwd, bwd)
    children += ch.asInstanceOf[ChildChannel[T, Any]] // TODO Get rid of cast
    if (!silent) flush(ch.process)
    ch
  }

  /** Redirect stream from ``other`` to ``this``. */
  def <<(other: ReadChannel[T]): ReadChannel[Unit] = {
    other.attach(this := _)
  }

  def <<[U](other: ReadChannel[T], ignore: ReadChannel[U]): ReadChannel[Unit] = {
    other.attach(produce(_, ignore))
  }

  def :=(v: T) = produce(v)
}

trait Channel[T] extends ReadChannel[T] with WriteChannel[T] {
  def toOpt: Opt[T] = {
    val res = Opt[T]()
    this <<>> res
    res
  }

  /** Synchronise ``this`` and ``other``. */
  def <<>>(other: Channel[T]) {
    var obsOther: ReadChannel[Unit] = null
    val obsThis = silentAttach(other.produce(_, obsOther))
    obsOther = other.attach(produce(_, obsThis))
    flush(obsThis.asInstanceOf[UniChildChannel[Any, Any]].process)
  }

  def <<>>(other: Channel[T], ignoreOther: ReadChannel[Unit]) {
    var obsOther: ReadChannel[Unit] = null
    val obsThis = silentAttach(other.produce(_, obsOther, ignoreOther))
    obsOther = other.attach(produce(_, obsThis))
    flush(obsThis.asInstanceOf[UniChildChannel[Any, Any]].process)
  }

  def +(write: WriteChannel[T]): Channel[T] = {
    val res = Channel[T]()
    val ignore = write << res
    this <<>> (res, ignore)
    res
  }

  /** @note Its public use is discouraged. takeUntil() is a safer alternative. */
  def dispose()
}

trait ChildChannel[T, U]
  extends Channel[U]
  with ChannelDefaultSize[U]
  with ChannelDefaultEmpty[U]
{
  private[widok] var disposable = false

  /** Return true if the stream is completed. */
  def process(value: T)
}

case class FlatChildChannel[T, U](parent: ReadChannel[T],
                                  observer: Channel.Observer[T, ReadChannel[U]])
  extends ChildChannel[T, U]
{
  private var bound: ReadChannel[U] = null

  def attached: Boolean =
    parent.children.contains(this.asInstanceOf[ChildChannel[T, Any]])

  def onChannel(ch: Option[ReadChannel[U]]) {
    if (bound != null) {
      bound.dispose()
      bound = null
    }

    if (ch.isDefined) {
      bound = ch.get
      bound.attach(this := _)
    }
  }

  def process(value: T) {
    observer(value) match {
      case Result.Next(resultValue) =>
        onChannel(resultValue)

      case Result.Done(resultValue) =>
        onChannel(resultValue)
        disposable = true
    }
  }

  def flush(f: U => Unit) {
    if (bound != null) bound.flush(f)
  }

  def dispose() {
    assert(attached)
    disposable = true

    if (bound != null) bound.dispose()

    children.foreach(_.dispose())
    children.clear()
  }
}

/** Uni-directional child */
case class UniChildChannel[T, U](parent: ReadChannel[T],
                                 observer: Channel.Observer[T, U])
  extends ChildChannel[T, U]
{
  def attached: Boolean =
    parent.children.contains(this.asInstanceOf[ChildChannel[T, Any]])

  def process(value: T) {
    assert(!disposable)

    observer(value) match {
      case Result.Next(resultValue) =>
        flatProduce(resultValue)
      case Result.Done(resultValue) =>
        flatProduce(resultValue)
        disposable = true
    }
  }

  def flush(f: U => Unit) {
    parent.flush(observer(_).valueOpt.foreach(f))
  }

  def dispose() {
    assert(attached)
    disposable = true

    children.foreach(_.dispose())
    children.clear()
  }
}

/** Bi-directional child */
case class BiChildChannel[T, U](parent: WriteChannel[T],
                                fwd: Channel.Observer[T, U],
                                bwd: Channel.Observer[U, T])
  extends ChildChannel[T, U]
{
  def attached: Boolean =
    parent.children.contains(this.asInstanceOf[ChildChannel[T, Any]])

  val back = silentAttach { value =>
    bwd(value) match {
      case Result.Next(resultValue) =>
        resultValue.foreach(r => parent.produce(r, this))
        Result.Next(None)
      case Result.Done(resultValue) =>
        resultValue.foreach(r => parent.produce(r, this))
        Result.Done(None)
    }
  }

  def process(value: T) {
    assert(!disposable)

    fwd(value) match {
      case Result.Next(resultValue) =>
        resultValue.foreach(r => produce(r, back))
      case Result.Done(resultValue) =>
        resultValue.foreach(r => produce(r, back))
        disposable = true
    }
  }

  def flush(f: U => Unit) {
    parent.flush(fwd(_).valueOpt.foreach(f))
  }

  def dispose() {
    assert(attached)
    disposable = true

    children.foreach(_.dispose())
    children.clear()
  }
}

trait ChannelDefaultSize[T] {
  this: ReadChannel[T] =>

  def size: ReadChannel[Int] = {
    var count = 0
    forkUni { t =>
      count += 1
      Result.Next(Some(count))
    }
  }
}

trait ChannelDefaultEmpty[T] {
  this: ReadChannel[T] =>

  def isEmpty: ReadChannel[Boolean] =
    forkUni { t =>
      Result.Done(Some(false))
    }

  def nonEmpty: ReadChannel[Boolean] =
    forkUni { t =>
      Result.Done(Some(true))
    }
}

trait RootChannel[T]
  extends Channel[T]
  with ChannelDefaultSize[T]
  with ChannelDefaultEmpty[T]
{
  def attached: Boolean = false

  def dispose() {
    children.foreach(_.dispose())
    children.clear()
  }
}

trait StateChannel[T] extends Channel[T] {
  def attached: Boolean = false

  def update(f: T => T) {
    flush(t => this := f(t))
  }

  /** Maps each value change of ``other`` to a change of ``this``. */
  def zip[U](other: ReadChannel[U]): ReadChannel[(T, U)] = {
    val res = Channel[(T, U)]()
    other.attach(u => flush(t => res := (t, u)))
    res
  }

  def value[U](f: shapeless.Lens[T, T] => shapeless.Lens[T, U]) =
    lens(f(shapeless.lens[T]))

  /** Two-way lens that propagates back changes to all observers. */
  def lens[U](l: shapeless.Lens[T, U]): Channel[U] = {
    var cur: Option[T] = None
    forkBi(
      fwdValue => { cur = Some(fwdValue); Result.Next(Some(l.get(fwdValue))) },
      bwdValue => Result.Next(Some(l.set(cur.get)(bwdValue))))
  }

  def dispose() {
    children.foreach(_.dispose())
    children.clear()
  }
}