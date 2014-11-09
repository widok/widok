package org.widok

import scala.collection.mutable
import cgta.otest.FunSuite

object AggregateSpec extends FunSuite {
  import ChannelSpec._

  def forallBuf[T](f: VarBuf[Int] => (ReadChannel[T], ReadChannel[T])) {
    val elems = Seq(1, 2, 3)

    val varbuf = VarBuf[Int]()

    elems.foreach { elem =>
      val (lch, rch) = f(varbuf)
      varbuf += elem
      assertEquals(lch, rch)
      tick()
    }

    val varbuf2 = VarBuf[Int]()
    val (lch, rch) = f(varbuf2)
    assertEquals(lch, rch)
    tick()
  }

  def forallBufSeq[T](f: VarBuf[Int] => (Seq[ReadChannel[T]], Seq[ReadChannel[T]])): Unit = {
    val elems = Seq(1, 2, 3)

    val varbuf = VarBuf[Int]()

    /** TODO Also check deletions and updates. */
    elems.foreach { elem =>
      val (lch, rch) = f(varbuf)
      varbuf += elem
      Assert.equals(lch.size, rch.size)
      lch.zip(rch).foreach { case (a, b) =>
        assertEquals(a, b)
        tick()
      }
    }
  }

  test("head") {
    forallBuf(varbuf => (varbuf.head.isEmpty, varbuf.isEmpty))
  }

  test("headOption") {
    forallBuf(varbuf => (varbuf.headOption.partialMap { case Some(v) => v }, varbuf.head))
  }

  test("lastOption") {
    forallBuf(varbuf => (varbuf.lastOption.partialMap { case Some(v) => v }, varbuf.last))
  }

  test("last") {
    forallBuf(varbuf => (varbuf.last.isEmpty, varbuf.isEmpty))
  }

  test("map") {
    forallBufSeq(varbuf => (varbuf.map(_ * 3).toSeq, varbuf.toSeq.map(_.map(_ * 3))))
  }
}
