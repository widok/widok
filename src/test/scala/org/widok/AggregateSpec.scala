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
}
