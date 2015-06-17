package org.widok

import minitest._

object BufferSpec extends SimpleTestSuite {
  import ChannelSpec._

  def forallBuf[T](f: Buffer[Int] => (ReadChannel[T], ReadChannel[T])) {
    def emptyChannel() {
      val buffer = Buffer[Int]()
      val (lch, rch) = f(buffer)
      assertEqualsCh(lch, rch)
    }

    def nonEmptyChannel() {
      val buffer = Buffer[Int]()

      (1 to 3).foreach { elem =>
        val (lch, rch) = f(buffer)
        buffer += elem
        assertEqualsCh(lch, rch)
      }
    }

    emptyChannel()
    nonEmptyChannel()
  }

  def forallBufSeq[T](f: Buffer[Int] => (ReadBuffer[T], () => Seq[T])) {
    val elems = Seq(1, 2, 3)

    def testInsert() {
      /** Set up handler before insertion */
      val buffer = Buffer[Int]()
      elems.foreach { elem =>
        val (buf, seq) = f(buffer)
        buffer += elem
        assertEquals(buf.get, seq())
      }
    }

    def testInsert2() {
      /** Set up handler after insertion */
      val buffer = Buffer[Int]()
      elems.foreach { elem =>
        buffer += elem
        val (buf, seq) = f(buffer)
        assertEquals(buf.get, seq())
      }
    }

    def testInsertAfter() {
      val buffer = Buffer[Int]()
      elems.foreach { elem =>
        val (buf, seq) = f(buffer)

        buffer += elem
        buffer += elem * 10
        buffer.insertAfter(elem, elem * 100)

        assertEquals(buf.get, seq())
      }
    }

    def testInsertBefore() {
      val buffer = Buffer[Int]()
      elems.foreach { elem =>
        val (buf, seq) = f(buffer)

        buffer += elem
        buffer += elem * 10
        buffer.insertBefore(elem * 10, elem * 100)

        assertEquals(buf.get, seq())
      }
    }

    def testDelete() {
      val buffer = Buffer[Int]()
      elems.foreach { elem =>
        val (buf, seq) = f(buffer)

        buffer += elem
        buffer += elem
        assertEquals(buf.get, seq())

        buffer -= elem /* First occurrence */
        assertEquals(buf.get, seq())
      }
    }

    def testReplace() {
      val buffer = Buffer[Int]()
      elems.foreach { elem =>
        val (buf, seq) = f(buffer)

        buffer += elem
        buffer += elem
        assertEquals(buf.get, seq())

        buffer.replace(elem, 0) /* First occurrence */
        assertEquals(buf.get, seq())

        buffer.replace(elem, 99) /* Second occurrence */
        assertEquals(buf.get, seq())
      }
    }

    /** TODO Display these sub-tasks in MiniTest as well */
    testInsert()
    testInsert2()
    testInsertAfter()
    testInsertBefore()
    testDelete()
    testReplace()
  }

  test("head") {
    forallBuf(buffer => (buffer.head.isEmpty, buffer.isEmpty))
  }

  test("headOption") {
    forallBuf(buffer => (buffer.headOption.values, buffer.head))
    forallBuf(buffer => (buffer.headOption.partialMap { case Some(v) => v }, buffer.head))
  }

  test("lastOption") {
    forallBuf(buffer => (buffer.lastOption.values, buffer.last))
    forallBuf(buffer => (buffer.lastOption.partialMap { case Some(v) => v }, buffer.last))
  }

  test("last") {
    forallBuf(buffer => (buffer.last.isEmpty, buffer.isEmpty))
  }

  test("map") {
    forallBufSeq(buffer => (buffer.map(_ * 3).buffer, () => buffer.get.map(_ * 3)))
  }

  test("filter") {
    forallBufSeq(buffer => (buffer.filter(_ > 1).buffer, () => buffer.get.filter(_ > 1)))
  }

  test("find") {
    forallBufSeq(buffer => (buffer.find(_ > 1).values.toBuffer, () => buffer.get.find(_ > 1).toSeq))
  }
}