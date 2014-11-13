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

  def forallBufSeq[T](f: VarBuf[Int] => (Seq[ReadChannel[T]], () => Seq[T])): Unit = {
    val elems = Seq(1, 2, 3)

    val varbuf = VarBuf[Int]()

    /** Set up handler before insertion */
    elems.foreach { elem =>
      val (lch, fr) = f(varbuf)
      varbuf += elem
      val r = fr()
      Assert.equals(lch.size, r.size)
      lch.zip(r).foreach { case (a, b) =>
        assertConstantEquals(a, b)
        tick()
      }
    }

    /** Set up handler after insertion */
    elems.foreach { elem =>
      varbuf += elem
      val (lch, fr) = f(varbuf)
      val r = fr()
      Assert.equals(lch.size, r.size)
      lch.zip(r).foreach { case (a, b) =>
        assertConstantEquals(a, b)
        tick()
      }
    }

    /** Inserting after */
    varbuf.clear()
    elems.foreach { elem =>
      val (lch, fr) = f(varbuf)

      val fst = varbuf += elem
      varbuf += elem + 1
      varbuf.insertAfter(fst, elem + 2)

      val r = fr()
      Assert.equals(lch.size, r.size)

      lch.zip(r).foreach { case (a, b) =>
        assertConstantEquals(a, b)
        tick()
      }
    }

    /** Inserting before */
    elems.foreach { elem =>
      val (lch, fr) = f(varbuf)

      val fst = varbuf += elem
      val snd = varbuf.insertBefore(fst, elem + 1)

      val r = fr()
      Assert.equals(lch.size, r.size)

      lch.zip(r).foreach { case (a, b) =>
        assertConstantEquals(a, b)
        tick()
      }
    }

    /** Deleting */
    elems.foreach { elem =>
      val (lch, fr) = f(varbuf)

      val fst = varbuf += elem
      val snd = varbuf += elem

      val r = fr()
      Assert.equals(lch.size, r.size)

      varbuf -= fst
      val rAfter = fr()
      Assert.equals(lch.size, rAfter.size)

      lch.zip(rAfter).foreach { case (a, b) =>
        assertConstantEquals(a, b)
        tick()
      }
    }

    /** TODO Also check updating. */
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
    forallBufSeq(varbuf => (varbuf.map(_ * 3).materialise, () => varbuf.toSeq.map(_.get * 3)))
  }

  test("filter") {
    forallBufSeq(varbuf => (varbuf.filter(_ > 1).materialise, () => varbuf.toSeq.map(_.get).filter(_ > 1)))
  }
}