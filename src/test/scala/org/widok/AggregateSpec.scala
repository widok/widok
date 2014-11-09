package org.widok

import scala.collection.mutable
import cgta.otest.FunSuite

object AggregateSpec extends FunSuite {
  import ChannelSpec._

  def materialise[T](buf: ReadVarBuf[T]): Seq[Var[T]] = {
    import Aggregate._
    val res = mutable.ArrayBuffer.empty[Var[T]]
    buf.changes.attach {
      case Change.Insert(Position.Head(), element) => res.prepend(element)
      case Change.Insert(Position.Last(), element) => res += element
      case Change.Insert(Position.Before(before), element) =>
        res.insert(res.indexOf(before) - 1, element)
      case Change.Insert(Position.After(after), element) =>
        res.insert(res.indexOf(after), element)
      case Change.Remove(element) => res -= element
      case Change.Clear() => res.clear()
    }
    res
  }

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

    /** Set up handler before insertion */
    elems.foreach { elem =>
      val (lch, rch) = f(varbuf)
      varbuf += elem
      Assert.equals(lch.size, rch.size)
      lch.zip(rch).foreach { case (a, b) =>
        assertEquals(a, b)
        tick()
      }
    }

    /** Set up handler after insertion */
    elems.foreach { elem =>
      varbuf += elem
      val (lch, rch) = f(varbuf)
      Assert.equals(lch.size, rch.size)
      lch.zip(rch).foreach { case (a, b) =>
        assertEquals(a, b)
        tick()
      }
    }

    /** Deleting */
    elems.foreach { elem =>
      val (lch, rch) = f(varbuf)

      val fst = varbuf += elem
      val snd = varbuf += elem

      val sizeBefore = (lch.size, rch.size)
      Assert.equals(lch.size, rch.size)

      varbuf -= fst
      val sizeAfter = (lch.size, rch.size)

      Assert.equals(lch.size, rch.size)
      Assert.isNotEquals(sizeBefore, sizeAfter)

      lch.zip(rch).foreach { case (a, b) =>
        assertEquals(a, b)
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
    forallBufSeq(varbuf => (materialise(varbuf.map(_ * 3)), varbuf.toSeq.map(_.map(_ * 3))))
  }
}