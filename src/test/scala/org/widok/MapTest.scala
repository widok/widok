package org.widok

import cgta.otest.FunSuite

object MapTest extends FunSuite {
  test("size()") {
    val buf = Buffer(1, 2, 3)
    val map = buf.mapTo(_ => -1)

    var size = 0
    map.size.attach(size = _)
    Assert.isEquals(size, 3)

    buf += 4
    Assert.isEquals(size, 4)
  }

  test("forall()") {
    val buf = Buffer(1, 2, 3)
    val map = buf.mapTo(_ => -1)

    var state = false
    map.forall(_ != -1).attach(state = _)
    Assert.isEquals(state, false)

    map.update(buf.get(0), 1)
    Assert.isEquals(state, false)

    map.update(buf.get(1), 2)
    Assert.isEquals(state, false)

    map.update(buf.get(2), 3)
    Assert.isEquals(state, true)
  }

  test("mapToCh()") {
    val buf = Buffer(0, 1, 2)
    val chs = List(Var("a"), Var("b"), Var("c"))
    val map = buf.mapToCh[String] { value =>
      chs(value).map(str => if (str.nonEmpty) Some(str) else None)
    }

    Assert.isEquals(map.values, Seq("a", "b", "c"))
    chs(1) := ""
    Assert.isEquals(map.values, Seq("a", "c"))
  }
}
