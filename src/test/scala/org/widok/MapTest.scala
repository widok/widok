package org.widok

import cgta.otest.FunSuite

object MapTest extends FunSuite {
  test("size()") {
    val buf = VarBuf(1, 2, 3)
    val map = buf.toOptMap[Int]

    var size = 0
    map.size.attach(size = _)
    Assert.isEquals(size, 3)

    buf += 4
    Assert.isEquals(size, 4)
  }
}
