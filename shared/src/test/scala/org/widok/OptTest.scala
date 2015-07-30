package org.widok

import minitest._

import scala.collection.mutable

object OptTest extends SimpleTestSuite {
  test("foldLeft()") {
    val elems = mutable.ArrayBuffer.empty[Int]

    val ch = Opt(0)
    val count = ch.foldLeft(0) { case (acc, cur) => acc + 1 }

    count.attach(elems += _)
    count.attach(elems += _)

    assertEquals(elems, mutable.ArrayBuffer(1, 1))
  }

  test("flatMap()") {
    val elems = mutable.ArrayBuffer.empty[String]

    val ch = Opt(0)
    val x = Channel[String]()
    val y = ch.values.flatMap(cur => x.map(_ + cur))

    y.attach(elems += _)

    x := "a"

    ch := 1
    x := "b"

    ch := 2
    x := "c"

    assertEquals(elems, mutable.ArrayBuffer("a0", "b1", "c2"))

    /* flatMap() must work with multiple attaches */
    val elems2 = mutable.ArrayBuffer.empty[String]
    y.attach(elems2 += _)
    assertEquals(elems, mutable.ArrayBuffer("a0", "b1", "c2"))
    x := "c"
    assertEquals(elems, mutable.ArrayBuffer("a0", "b1", "c2", "c2"))
    assertEquals(elems2, mutable.ArrayBuffer("c2"))
  }

  test("size()") {
    val elems = mutable.ArrayBuffer.empty[Int]

    val ch = Opt("a")

    val size = ch.size
    size.attach(elems += _)

    ch.clear()
    ch := "b"
    ch := "c"

    assertEquals(elems, mutable.ArrayBuffer(1, 0, 1, 2))
  }

  test("values()") {
    val elems = mutable.ArrayBuffer.empty[Option[Int]]

    val ch = Opt[Int]()
    ch.attach(elems += _)

    ch := 1
    ch.clear()
    ch := 2

    assertEquals(elems, mutable.ArrayBuffer(None, Some(1), None, Some(2)))
  }
}
