package org.widok

import minitest._

object ArrayTest extends SimpleTestSuite {
  test("append, remove") {
    val arr = Array[Int]()

    arr += 1
    assertEquals(arr.size, 1)
    assertEquals(arr.isEmpty, false)
    assertEquals(arr.elements.toList, List(1))

    arr -= 1
    assertEquals(arr.size, 0)
    assertEquals(arr.isEmpty, true)
    assertEquals(arr.elements.toList, List())
  }

  test("clear") {
    val arr = Array[Int]()
    arr += 1
    arr += 2
    arr.clear()
    assertEquals(arr.isEmpty, true)
  }
}
