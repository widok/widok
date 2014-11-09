package org.widok

import scala.collection.mutable
import cgta.otest.FunSuite

object ChannelSpec extends FunSuite {
  var tickExpr: () => Unit = () => ()

  def tick() {
    tickExpr()
    tickExpr = () => ()
  }

  /** Checks whether two channels behave in the same way. */
  def assertEquals[T](ch: ReadChannel[T], ch2: ReadChannel[T], parent: Option[Channel[Int]] = None) {
    val left = mutable.ArrayBuffer[T]()
    val right = mutable.ArrayBuffer[T]()

    tickExpr = () => {
      Assert.isEquals(left, right,
        "Both channels produce the same values",
        "Channel type: " + parent.map(_.getClass))
    }

    ch.attach { value => left += value }
    ch2.attach { value => right += value }
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
        assertEquals(lch, rch, Some(ch))
        ch := value
        tick()
      }

      /* Produce a value and check if law holds when the argument is a different
       * value that was not produced before.
       */
      val ch2 = fch()
      elems.foreach { value =>
        val (lch, rch) = f(ch2, value * 2)
        assertEquals(lch, rch, Some(ch))
        ch2 := value
        tick()
      }
    }
  }

  def forallCh[T](f: Channel[Int] => (ReadChannel[T], ReadChannel[T])): Unit = {
    /* Check whether law holds for no values produced. */
    val ch = Channel[Int]()
    val (lch, rch) = f(ch)
    assertEquals(lch, rch)
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
    assertEquals(Var(42).head, Var(42))
    forallCh(ch => (ch.head, ch.take(1)))
  }

  test("distinct") {
    forallCh(ch => (ch.head.distinct, ch.head))
  }

  test("isEmpty") {
    assertEquals(Var(42).isEmpty, Var(false))
    assertEquals(Opt().isEmpty, Var(true))
    forallCh(ch => (ch.isEmpty, ch.nonEmpty.map(!_)))
    forallCh(ch => (ch.nonEmpty, ch.head.map(_ => true)))
  }

  test("size") {
    assertEquals(Var(42).size, Var(1))
    assertEquals(Opt().size, Var(0))
    assertEquals(Opt(1).size, Var(1))
    forallCh(ch => (ch.size, ch.foldLeft(0) { case (acc, cur) => acc + 1 }))
  }

  test("Opt") {
    forallCh(ch => (ch.toOpt, ch))

    assertEquals(Opt().isDefined.head, Var(false))
    assertEquals(Opt(42).isDefined.head, Var(true))

    assertEquals(Opt().isDefined, Opt().nonEmpty)
    assertEquals(Opt(42).isDefined, Opt(42).nonEmpty)

    assertEquals(Opt(42), Var(42))
    assertEquals(Opt(), Channel())
  }
}
