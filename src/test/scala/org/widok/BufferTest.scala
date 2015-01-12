package org.widok

import minitest._

object BufferTest extends SimpleTestSuite {
  test("forall()") {
    val buf = Buffer(1, 2, 3)

    var state = false
    buf.forall(_ > 0).attach(state = _)
    assertEquals(state, true)

    buf += 0
    assertEquals(state, false)

    buf.remove(buf.get.last)
    assertEquals(state, true)
  }

  test("filter().lastOption") {
    val buf = Buffer[Int]()
    val filter = buf.filter(_ > 1)
    var last = -1

    filter.lastOption.attach(cur ⇒ last = cur.get.get)

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
    assertEquals(x.get, y.get)
  }

  test("flatMapCh()") {
    /* Ensure that references are preserved. */
    val x = Buffer(1, 2, 3)
    val y = x.flatMapCh[Int](value => x.watch(value))
    assertEquals(x.get, y.get)

    val fst = x.get.head
    x.remove(fst)
    x.prepend(fst)
    assertEquals(x.get, y.get)
  }

  test("removeAll()") {
    val x = Buffer(1, 2, 3)
    val add = Buffer(4, 5, 6)
    x ++= add
    val y = x.filter(_ <= 3)

    x.removeAll(y)

    assertEquals(x.get, add.get)
  }
}
