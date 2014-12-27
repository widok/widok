package org.widok

import minitest._

object MapTest extends SimpleTestSuite {
  test("size()") {
    val buf = Buffer(1, 2, 3)
    val map = buf.mapTo(_ => -1)

    var size = 0
    map.size.attach(size = _)
    assertEquals(size, 3)

    buf += 4
    assertEquals(size, 4)
  }

  test("forall()") {
    val buf = Buffer(1, 2, 3)
    val map = buf.mapTo(_ => -1)

    var state = false
    map.forall(_ != -1).attach(state = _)
    assertEquals(state, false)

    map.update(buf.get(0), 1)
    assertEquals(state, false)

    map.update(buf.get(1), 2)
    assertEquals(state, false)

    map.update(buf.get(2), 3)
    assertEquals(state, true)
  }

  test("mapToCh()") {
    val buf = Buffer(0, 1, 2)
    val chs = List(Var("a"), Var("b"), Var("c"))
    val map = buf.mapToCh[String] { value =>
      chs(value).map(str => if (str.nonEmpty) Some(str) else None)
    }

    assertEquals(map.values, Seq("a", "b", "c"))
    chs(1) := ""
    assertEquals(map.values, Seq("a", "c"))
  }
}
