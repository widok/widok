package org.widok

import cgta.otest.FunSuite

object VarBufTest extends FunSuite {
  test("forall()") {
    val buf = VarBuf(1, 2, 3)

    var state = false
    buf.forall(_ > 0).attach(state = _)
    Assert.isEquals(state, true)

    val elem = buf += 0
    Assert.isEquals(state, false)

    elem := 1
    Assert.isEquals(state, true)
  }

  test("filter().lastOption") {
    val buf = VarBuf[Int]()
    val filter = buf.filter(_ > 1)
    var last = -1

    filter.lastOption.attach(cur ⇒ last = cur.get.get)

    buf += 1
    buf += 2
    buf += 3

    Assert.isEquals(last, 3)
    buf.remove(buf(2))

    Assert.isEquals(last, 2)
  }

  test("size()") {
    var cur = -1
    VarBuf().size.attach(cur = _)
    Assert.isEquals(cur, 0)
  }
}
