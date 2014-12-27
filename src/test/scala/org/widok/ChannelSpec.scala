package org.widok

import minitest._

import scala.collection.mutable

object ChannelSpec extends SimpleTestSuite {
  var tickExpr: () => Unit = () => ()

  def tick() {
    tickExpr()
    tickExpr = () => ()
  }

  /** Checks whether two channels behave in the same way. */
  def assertEqualsCh[T](ch: ReadChannel[T], ch2: ReadChannel[T], parent: Option[Channel[Int]] = None) {
    val left = mutable.ArrayBuffer[T]()
    val right = mutable.ArrayBuffer[T]()

    tickExpr = () => {
      assert(left == right,
        "Both channels produce the same values; " +
        "Channel type: " + parent.map(_.getClass))
    }

    ch.attach { value => left += value }
    ch2.attach { value => right += value }
  }

  /** Checks whether ``ch`` contains the given value. */
  def assertConstantEquals[T](ch: ReadChannel[T], value: T) {
    val left = mutable.ArrayBuffer[T]()

    tickExpr = () =>
      assert(left == mutable.ArrayBuffer(value), "Channel produces one value")

    ch.attach { value => left += value }
  }

  def forallChVal[T](f: (Channel[Int], Int) => (ReadChannel[T], ReadChannel[T])) {
    /* Different channel types may differ in their semantics. */
    val channels = Seq(
      () => Var[Int](0),
      () => Opt[Int](0),
      () => Channel[Int]())
    val elems = Seq(1, 2, 3)

    channels.foreach { fch =>
      /* Produce a value and check if law holds for it. */
      val ch = fch()
      elems.foreach { value =>
        val (lch, rch) = f(ch, value)
        assertEqualsCh(lch, rch, Some(ch))
        ch := value
        tick()
      }

      /* Produce a value and check if law holds when the argument is a different
       * value that was not produced before.
       */
      val ch2 = fch()
      elems.foreach { value =>
        val (lch, rch) = f(ch2, value * 2)
        assertEqualsCh(lch, rch, Some(ch))
        ch2 := value
        tick()
      }
    }
  }

  def forallCh[T](f: Channel[Int] => (ReadChannel[T], ReadChannel[T])): Unit = {
    /* Check whether law holds for no values produced. */
    val ch = Channel[Int]()
    val (lch, rch) = f(ch)
    assertEqualsCh(lch, rch)
    tick()

    forallChVal((ch, _) => f(ch))
  }

  test("contains") {
    forallChVal((ch, value) => (ch.contains(value), ch.exists(_ == value)))
  }

  test("equal") {
    forallChVal((ch, value) => (ch.equal(value), ch.unequal(value).map(!_)))
  }

  /* TODO Generalise values */
  test("head") {
    // TODO Use Channel.fromSeq()
    assertEqualsCh(Var(42).head, Var(42))
    forallCh(ch => (ch.head, ch.take(1)))
  }

  test("distinct") {
    forallCh(ch => (ch.head.distinct, ch.head))
  }

  test("isEmpty") {
    assertEqualsCh(Var(42).isEmpty, Var(false))
    assertEqualsCh(Opt().isEmpty, Var(true))
    forallCh(ch => (ch.isEmpty, ch.nonEmpty.map(!_)))
    forallCh(ch => (ch.nonEmpty, ch.head.map(_ => true)))
  }

  test("size") {
    assertEqualsCh(Var(42).size, Var(1))
    assertEqualsCh(Opt().size, Var(0))
    assertEqualsCh(Opt(1).size, Var(1))
    forallCh(ch => (ch.size, ch.foldLeft(0) { case (acc, cur) => acc + 1 }))
  }

  test("Opt") {
    forallCh(ch => (ch.toOpt, ch))

    assertEqualsCh(Opt().isDefined.head, Var(false))
    assertEqualsCh(Opt(42).isDefined.head, Var(true))

    assertEqualsCh(Opt().isDefined, Opt().nonEmpty)
    assertEqualsCh(Opt(42).isDefined, Opt(42).nonEmpty)

    assertEqualsCh(Opt(42), Var(42))
    assertEqualsCh(Opt(), Channel())
  }
}
