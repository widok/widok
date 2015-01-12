package org.widok

object Channel {
  type Observer[T, U] = T => Result[U]

  def apply[T](): Channel[T] =
    new RootChannel[T] {
      def flush(f: T => Unit) { }
    }

  /** Combine a read with a write channel. */
  def apply[T](read: ReadChannel[T], write: WriteChannel[T]): Channel[T] = {
    val res = new RootChannel[T] {
      def flush(f: T => Unit) { read.flush(f) }
    }
    val pub = res.publish(write)
    res.subscribe(read, pub)
    res
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
  extends StreamFunctions[ReadChannel, T]
  with FilterFunctions[ReadChannel, T]
  with FoldFunctions[T]
  with MapFunctions[ReadChannel, T]
  with IterateFunctions[T]
  with SizeFunctions
  with Disposable
{
  import Channel.Observer

  private[widok] val children = Array[ChildChannel[T, _]]()

  def cache: Opt[T] = {
    val res = Opt[T]()
    res << this
    res
  }

  def cache(default: T): Var[T] = {
    val res = Var[T](default)
    res << this
    res
  }

  /** Flush data and call f for each element */
  def flush(f: T => Unit)

  def publish(ch: WriteChannel[T]): ReadChannel[Unit] = ch.subscribe(this)
  def publish[U](ch: WriteChannel[T], ignore: ReadChannel[U]): ReadChannel[Unit] = ch.subscribe(this, ignore)
  def >>(ch: WriteChannel[T]) = publish(ch)

  def merge(ch: ReadChannel[T]): ReadChannel[T] = {
    val res = new RootChannel[T] {
      def flush(f: T => Unit) {
        ReadChannel.this.flush(t => this := t)
        ch.flush(t => this := t)
      }
    }

    res << this
    res << ch

    res
  }

  def child(): ReadChannel[T] =
    forkUni(t => Result.Next(Some(t)))

  def attach(f: T => Unit): ReadChannel[Unit] =
    forkUni { value =>
      f(value)
      Result.Next(None)
    }

  def detach(ch: ChildChannel[T, _]) {
    children -= ch
  }

  def silentAttach(f: T => Unit): ReadChannel[Unit] =
    forkUni(value => {
      f(value)
      Result.Next(None)
    }, silent = true)

  def buffer: Buffer[T] = {
    val buf = Buffer[T]()
    attach(value => buf.append(value))
    buf
  }

  /** Uni-directional fork for values */
  def forkUni[U](observer: Observer[T, U], silent: Boolean = false): ReadChannel[U] = {
    val ch = UniChildChannel[T, U](this, observer, None)
    children += ch
    if (!silent) flush(ch.process)
    ch
  }

  def forkUniState[U](observer: Observer[T, U], onFlush: => Option[U]): ReadChannel[U] = {
    val ch = UniChildChannel[T, U](this, observer, Some(() => onFlush))
    children += ch
    flush(ch.process)
    ch
  }

  /** Uni-directional fork for channels */
  def forkUniFlat[U](observer: Observer[T, ReadChannel[U]]): ReadChannel[U] = {
    val ch = FlatChildChannel[T, U](this, observer)
    children += ch
    flush(ch.process)
    ch
  }

  /** Bi-directional fork for channels */
  def forkBiFlat[U](obs: Observer[T, Channel[U]], silent: Boolean = false): Channel[U] = {
    val ch = BiFlatChildChannel[T, U](this, obs)
    children += ch
    if (!silent) flush(ch.process)
    ch
  }

  def filter(f: T => Boolean): ReadChannel[T] =
    forkUni { value =>
      if (f(value)) Result.Next(Some(value))
      else Result.Next(None)
    }

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
    forkUni(value => {
      if (cnt > 0) { cnt -= 1; Result.Next(None) }

      // TODO Create a new result type which continues processing
      // the stream without calling this callback.
      else Result.Next(Some(value))
    }, silent = true)
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
    forkUniFlat(value => Result.Next(Some(f(value))))

  /** flatMap with back-propagation. */
  def flatMapCh[U](f: T => Channel[U]): Channel[U] =
    forkBiFlat(value => Result.Next(Some(f(value))))

  def flatMapBuf[U](f: T => ReadBuffer[U]): ReadBuffer[U] = {
    val buf = Buffer[U]()
    var child: ReadChannel[Unit] = null
    attach { value =>
      buf.clear()
      if (child != null) child.dispose()
      child = f(value).changes.attach(buf.applyChange)
    }
    buf
  }

  def partialMap[U](f: PartialFunction[T, U]): ReadChannel[U] =
    forkUni { value =>
      Result.Next(f.lift(value))
    }

  /** @note Caches the accumulator value. */
  def foldLeft[U](acc: U)(f: (U, T) => U): ReadChannel[U] = {
    var accum = acc
    forkUniState(value => {
      accum = f(accum, value)
      Result.Next(Some(accum))
    }, Some(accum))
  }

  def find(f: T => Boolean): ReadChannel[Option[T]] = ???

  def exists(f: T => Boolean): ReadChannel[Boolean] =
    forkUni { value =>
      if (f(value)) Result.Done(Some(true))
      else Result.Next(None)
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
    val res = Channel[T]()
    val ignore = write << res
    res << (this, ignore)
    res
  }

  def distinct: ReadChannel[T] = {
    var cur = Option.empty[T]
    forkUniState(value => {
      if (cur.contains(value)) Result.Next(None)
      else {
        cur = Some(value)
        Result.Next(Some(cur.get))
      }
    }, cur)
  }
}

trait WriteChannel[T] {
  import Channel.Observer

  private[widok] val children: Array[ChildChannel[T, _]]

  def detach(ch: ChildChannel[T, _])

  def flush(f: T => Unit)

  def setter(value: T): WriteChannel[_] = {
    val ch = Channel()
    ch.attach(_ => this := value)
    ch
  }

  def produce(value: T) {
    children.foreach(_.process(value))
  }

  def produce[U](value: T, ignore: ReadChannel[U]*) {
    assume(ignore.forall(cur => children.contains(cur.asInstanceOf[ChildChannel[T, _]])))
    children.foreach { child =>
      if (!ignore.contains(child)) child.process(value)
    }
  }

  def flatProduce(value: Option[T]) {
    value.foreach(produce)
  }

  def flatProduce[U](value: Option[T], ignore: ReadChannel[U]*) {
    value.foreach(v => produce(v, ignore: _*))
  }

  /** Bi-directional fork for values */
  def forkBi[U](fwd: Observer[T, U], bwd: Observer[U, T], silent: Boolean = false): Channel[U] = {
    val ch = BiChildChannel[T, U](this, fwd, bwd)
    children += ch
    if (!silent) flush(ch.process)
    ch
  }

  /** Redirect stream from ``other`` to ``this``. */
  def subscribe(ch: ReadChannel[T]): ReadChannel[Unit] =
    ch.attach(this := _)

  def subscribe[U](ch: ReadChannel[T], ignore: ReadChannel[U]): ReadChannel[Unit] =
    ch.attach(produce(_, ignore))

  def <<(ch: ReadChannel[T]): ReadChannel[Unit] = subscribe(ch)
  def <<[U](ch: ReadChannel[T], ignore: ReadChannel[U]): ReadChannel[Unit] =
    subscribe(ch, ignore)
  def :=(v: T) = produce(v)
}

trait Channel[T] extends ReadChannel[T] with WriteChannel[T] {
  def toOpt: Opt[T] = {
    val res = Opt[T]()
    this <<>> res
    res
  }

  def biMap[U](f: T => U, g: U => T): Channel[U] =
    forkBi(
      fwdValue => Result.Next(Some(f(fwdValue))),
      bwdValue => Result.Next(Some(g(bwdValue))))

  def partialBiMap[U](f: T => Option[U], g: U => Option[T]): Channel[U] =
    forkBi(
      fwdValue => Result.Next(f(fwdValue)),
      bwdValue => Result.Next(g(bwdValue)))

  /** Two-way binding; synchronises ``this`` and ``other``. */
  def bind(other: Channel[T]) {
    var obsOther: ReadChannel[Unit] = null
    val obsThis = silentAttach(other.produce(_, obsOther))
    obsOther = other.attach(produce(_, obsThis))
    flush(obsThis.asInstanceOf[UniChildChannel[Any, Any]].process)
  }

  def bind(other: Channel[T], ignoreOther: ReadChannel[Unit]) {
    var obsOther: ReadChannel[Unit] = null
    val obsThis = silentAttach(other.produce(_, obsOther, ignoreOther))
    obsOther = other.attach(produce(_, obsThis))
    flush(obsThis.asInstanceOf[UniChildChannel[Any, Any]].process)
  }

  def <<>>(other: Channel[T]) { bind(other) }
  def <<>>(other: Channel[T], ignoreOther: ReadChannel[Unit]) { bind(other, ignoreOther) }

  def +(write: WriteChannel[T]): Channel[T] = {
    val res = new RootChannel[T] {
      def flush(f: T => Unit) { Channel.this.flush(f) }
    }
    val ignore = write << res
    this <<>> (res, ignore)
    res
  }

  /** @note Its public use is discouraged. takeUntil() is a safer alternative. */
  def dispose()

  override def toString = "Channel()"
}

trait ChildChannel[T, U]
  extends Channel[U]
  with ChannelDefaultSize[U]
  with ChannelDefaultEmpty[U]
{
  /** Return true if the stream is completed. */
  def process(value: T)
}

case class FlatChildChannel[T, U](parent: ReadChannel[T],
                                  observer: Channel.Observer[T, ReadChannel[U]])
  extends ChildChannel[T, U]
{
  private var bound: ReadChannel[U] = null
  private var subscr: ReadChannel[Unit] = null

  def onChannel(ch: Option[ReadChannel[U]]) {
    if (subscr != null) subscr.dispose()

    if (ch.isDefined) {
      bound = ch.get
      subscr = bound.attach(this := _)
    } else {
      subscr = null
      bound = null
    }
  }

  def process(value: T) {
    observer(value) match {
      case Result.Next(resultValue) =>
        onChannel(resultValue)

      case Result.Done(resultValue) =>
        onChannel(resultValue)
        dispose()
    }
  }

  def flush(f: U => Unit) {
    if (bound != null) bound.flush(f)
  }

  def dispose() {
    parent.detach(this)

    if (subscr != null) subscr.dispose()

    children.foreach(_.dispose())
    children.clear()
  }

  override def toString = "FlatChildChannel()"
}

/** Uni-directional child */
case class UniChildChannel[T, U](parent: ReadChannel[T],
                                 observer: Channel.Observer[T, U],
                                 onFlush: Option[() => Option[U]])
  extends ChildChannel[T, U]
{
  private var inProcess = false

  def process(value: T) {
    assert(!inProcess, "Cycle found")

    inProcess = true

    observer(value) match {
      case Result.Next(resultValue) =>
        flatProduce(resultValue)
      case Result.Done(resultValue) =>
        flatProduce(resultValue)
        dispose()
    }

    inProcess = false
  }

  def flush(f: U => Unit) {
    if (onFlush.isDefined) onFlush.get().foreach(f)
    else parent.flush(observer(_).valueOpt.foreach(f))
  }

  def dispose() {
    parent.detach(this)

    children.foreach(_.dispose())
    children.clear()
  }

  override def toString = "UniChildChannel()"
}

/** Bi-directional child */
case class BiChildChannel[T, U](parent: WriteChannel[T],
                                fwd: Channel.Observer[T, U],
                                bwd: Channel.Observer[U, T])
  extends ChildChannel[T, U]
{
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
    fwd(value) match {
      case Result.Next(resultValue) =>
        resultValue.foreach(r => produce(r, back))
      case Result.Done(resultValue) =>
        resultValue.foreach(r => produce(r, back))
        dispose()
    }
  }

  def flush(f: U => Unit) {
    parent.flush(fwd(_).valueOpt.foreach(f))
  }

  def dispose() {
    parent.detach(this)

    back.dispose()

    children.foreach(_.dispose())
    children.clear()
  }

  override def toString = "BiChildChannel()"
}

case class BiFlatChildChannel[T, U](parent: ReadChannel[T],
                                    observer: Channel.Observer[T, Channel[U]])
  extends ChildChannel[T, U]
{
  private var bound: Channel[U] = null
  private var subscr: ReadChannel[Unit] = null

  val back = silentAttach { value =>
    if (bound != null && subscr != null) bound.produce(value, subscr)
  }

  def onChannel(ch: Option[Channel[U]]) {
    if (subscr != null) subscr.dispose()

    if (ch.isDefined) {
      bound = ch.get
      subscr = bound.silentAttach(produce(_, back))
      bound.flush(produce(_, back))
    } else {
      bound = null
      subscr = null
    }
  }

  def process(value: T) {
    observer(value) match {
      case Result.Next(resultValue) =>
        onChannel(resultValue)

      case Result.Done(resultValue) =>
        onChannel(resultValue)
        dispose()
    }
  }

  def flush(f: U => Unit) {
    if (bound != null) bound.flush(f)
  }

  def dispose() {
    parent.detach(this)

    if (subscr != null) subscr.dispose()

    back.dispose()

    children.foreach(_.dispose())
    children.clear()
  }

  override def toString = "BiFlatChildChannel()"
}

trait ChannelDefaultSize[T] {
  this: ReadChannel[T] =>

  def size: ReadChannel[Int] =
    foldLeft(0) { case (acc, cur) => acc + 1}
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
  def dispose() {
    children.foreach(_.dispose())
    children.clear()
  }
}

trait StateChannel[T] extends Channel[T] {
  def update(f: T => T) {
    flush(t => this := f(t))
  }

  /** Maps each value change of ``other`` to a change of ``this``. */
  def zip[U](other: ReadChannel[U]): ReadChannel[(T, U)] = {
    val res = Channel[(T, U)]()
    other.attach(u => flush(t => res := (t, u)))
    res
  }

  /** In contrast to zip() this produces a new value for each change of
    * ``this`` or ``other``. Therefore, ``other`` must be a StateChannel.
    */
  def combine[U](other: StateChannel[U]): ReadChannel[(T, U)] = {
    val res = Channel[(T, U)]()
    attach(t => other.flush(u => res := (t, u)))
    other.attach(u => flush(t => res := (t, u)))
    res
  }

  // Shapeless has not been built yet for Scala.js 0.6.0
  /*def value[U](f: shapeless.Lens[T, T] => shapeless.Lens[T, U]) =
    lens(f(shapeless.lens[T]))

  /** Two-way lens that propagates back changes to all observers. */
  def lens[U](l: shapeless.Lens[T, U]): Channel[U] = {
    var cur: Option[T] = None
    forkBi(
      fwdValue => { cur = Some(fwdValue); Result.Next(Some(l.get(fwdValue))) },
      bwdValue => Result.Next(Some(l.set(cur.get)(bwdValue))))
  }*/

  def dispose() {
    children.foreach(_.dispose())
    children.clear()
  }
}