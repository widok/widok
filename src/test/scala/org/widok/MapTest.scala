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
}
