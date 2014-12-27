package org.widok

import minitest._

object RefTest extends SimpleTestSuite {
  test("==") {
    assert(Ref(1) != Ref(1))

    val ref = Ref(1)
    val ref2 = ref
    assert(ref == ref2)
  }

  test("matching") {
    Ref(1) match {
      case Ref(value) => assert(value == 1)
      case _ => assert(false)
    }
  }
}
