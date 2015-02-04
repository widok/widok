package org.widok

import minitest._

import scala.collection.mutable

object BufferTest extends SimpleTestSuite {
  test("forall()") {
    val buf = Buffer(1, 2, 3)

    var states = mutable.ArrayBuffer.empty[Boolean]
    buf.forall(_ > 0).attach(states += _)
    assertEquals(states, Seq(true))

    buf += 0
    assertEquals(states, Seq(true, false))

    buf.remove(buf.get.last)
    assertEquals(states, Seq(true, false, true))
  }

  test("filter().lastOption") {
    val buf = Buffer[Int]()
    val filter = buf.filter(_ > 1)
    var last = -1

    filter.buffer.lastOption.attach(last = _)

    buf += 1
    buf += 2
    buf += 3

    assertEquals(last, 3)
    buf.remove(buf.get(2))

    assertEquals(last, 2)
  }

  test("size()") {
    var cur = -1
    Buffer().size.attach(cur = _)
    assertEquals(cur, 0)
  }

  test("concat()") {
    /* Ensure that references are preserved. */
    val x = Buffer(1, 2, 3)
    val y = x.concat(Buffer())
    assertEquals(x.get, y.buffer.get)
  }

  test("flatMapCh()") {
    /* Ensure that references are preserved. */
    val x = Buffer(1, 2, 3)
    val y = x.flatMapCh[Int](value => Var(Some(value)))
    assertEquals(x.get, y.buffer.get)

    val fst = x.get.head
    x.remove(fst)
    x.prepend(fst)
    assertEquals(x.get, y.buffer.get)
  }

  test("removeAll()") {
    val x = Buffer(1, 2, 3)
    val add = Buffer(4, 5, 6)

    x ++= add
    x.removeAll(add)

    assertEquals(x.get, Seq(1, 2, 3))
  }

  test("removeAll()") {
    val x = Buffer(1, 2, 3)
    val add = Buffer(4, 5, 6)

    x ++= add

    val y = x.filter(_ <= 3).buffer
    x.removeAll(y)

    assertEquals(x.get, add.get)
  }
}
