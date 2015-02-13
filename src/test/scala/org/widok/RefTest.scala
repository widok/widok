package org.widok

import minitest._

object RefTest extends SimpleTestSuite {
  test("==") {
    assert(Ref(1) != Ref(1))

    val ref = Ref(1)
    val ref2 = ref
    assertEquals(ref, ref2)
  }

  test("matching") {
    Ref(1) match {
      case Ref(value) => assertEquals(value, 1)
      case _ => assert(false)
    }
  }

  test("Var") {
    val var1 = Var(1)
    val var2 = Var(1)
    assert(var1 != var2)
  }
}
