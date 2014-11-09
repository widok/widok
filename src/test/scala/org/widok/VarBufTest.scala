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
}
