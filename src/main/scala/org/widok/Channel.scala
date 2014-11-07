package org.widok

import scala.collection.mutable

import org.widok.Helpers.Identity

object Channel {
  type Observer[T, U] = T => Result[U]

  def apply[T](): Channel[T] =
    new Channel[T] {
      def request() { }
      def flush(f: T => Unit) { }
      def attached: Boolean = false
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
  with Disposable {

  import Channel.Observer

  private[widok] val children = mutable.Queue[ChildChannel[T, Any]]()

  def flush(f: T => Unit)

  def request()

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
    if (!silent) ch.request()
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

  def flatMap[U](f: T => ReadChannel[U]): ReadChannel[U] = {
    val res = Channel[U]()
    attach(f(_).attach(res := _))
    res
  }

  // TODO Not covered by flatMap() in ``MapFunctions``. Probably ``Channel`` needs to
  // be co-variant.
  def flatMapCh[U](f: T => Channel[U]): Channel[U] = ???

  def partialMap[U](f: PartialFunction[T, U]): ReadChannel[U] =
    forkUni { value =>
      Result.Next(f.lift(value))
    }

  def foldLeft[U](acc: U)(f: (U, T) => U): ReadChannel[U] = {
    var accum = acc
    forkUni { value =>
      accum = f(accum, value)
      Result.Next(Some(accum))
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

  def takeUntil(ch: Channel[_]): ReadChannel[T] = {
    val res = Channel[T]()
    val child = forkUni[T] { value =>
      res := value
      Result.Next(None)
    }
    ch.forkUni[Any] { _ =>
      child.dispose()
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
    children.dequeueAll(_.process(value))
  }

  def produce[U](value: T, ignore: ReadChannel[U]*) {
    assume(ignore.forall(children.contains))
    children.diff(ignore).dequeueAll(_.process(value))
  }

  def flatProduce(v: Option[T]) {
    v.foreach(cur => children.dequeueAll(_.process(cur)))
  }

  def flatProduce[U](v: Option[T], ignore: Observer[T, U]*) {
    assume(ignore.forall(children.contains))
    v.foreach(cur => children.diff(ignore).dequeueAll(_.process(cur)))
  }

  /** Bi-directional fork */
  def forkBi[U](fwd: Observer[T, U], bwd: Observer[U, T], silent: Boolean = false): Channel[U] = {
    val ch = BiChildChannel[T, U](this, fwd, bwd)
    children += ch.asInstanceOf[ChildChannel[T, Any]] // TODO Get rid of cast
    if (!silent) ch.request()
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
    obsThis.request()
  }

  def <<>>(other: Channel[T], ignoreOther: ReadChannel[Unit]) {
    var obsOther: ReadChannel[Unit] = null
    val obsThis = silentAttach(other.produce(_, obsOther, ignoreOther))
    obsOther = other.attach(produce(_, obsThis))
    obsThis.request()
  }

  def +(write: WriteChannel[T]): Channel[T] = {
    val res = Channel[T]()
    val ignore = write << res
    this <<>> (res, ignore)
    res
  }

  /** @note Its public use is discouraged. takeUntil() is a safer alternative. */
  def dispose() {
    assert(!attached)
    children.dequeueAll { cur =>
      cur.dispose()
      true
    }
  }
}

trait ChildChannel[T, U] extends Channel[U] {
  def process(value: T): Boolean
}

/** Uni-directional child */
case class UniChildChannel[T, U](parent: ReadChannel[T],
                                 observer: Channel.Observer[T, U]) extends ChildChannel[T, U] {
  def attached: Boolean =
    parent.children.contains(this.asInstanceOf[ChildChannel[T, Any]])

  def process(value: T): Boolean =
    observer(value) match {
      case Result.Next(resultValue) =>
        flatProduce(resultValue)
        false
      case Result.Done(resultValue) =>
        flatProduce(resultValue)
        true
    }

  /** Flush data from parent */
  def request() {
    parent.flush(value => process(value))
  }

  def flush(f: U => Unit) {
    parent.flush(observer(_).valueOpt.foreach(f))
  }
}

/** Bi-directional child */
case class BiChildChannel[T, U](parent: WriteChannel[T],
                                fwd: Channel.Observer[T, U],
                                bwd: Channel.Observer[U, T]) extends ChildChannel[T, U]
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

  def request() {
    parent.flush(value => process(value))
  }

  def process(value: T): Boolean =
    fwd(value) match {
      case Result.Next(resultValue) =>
        resultValue.foreach(r => produce(r, back))
        false
      case Result.Done(resultValue) =>
        resultValue.foreach(r => produce(r, back))
        true
    }

  def flush(f: U => Unit) {
    parent.flush(fwd(_).valueOpt.foreach(f))
  }
}

trait StateChannel[T] extends Channel[T] {
  def attached: Boolean = false

  def request() { }

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
}