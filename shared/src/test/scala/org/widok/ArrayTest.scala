package org.widok

import minitest._

object ArrayTest extends SimpleTestSuite {
  test("append, remove") {
    val arr = Array[Int]()

    arr += 1
    assertEquals(arr.size, 1)
    assertEquals(arr.isEmpty, false)
    assertEquals(arr.toList, List(1))

    arr -= 1
    assertEquals(arr.size, 0)
    assertEquals(arr.isEmpty, true)
    assertEquals(arr.toList, List())
  }

  test("clear") {
    val arr = Array[Int]()
    arr += 1
    arr += 2
    arr.clear()
    assertEquals(arr.isEmpty, true)
  }

  test("foreach with in-place modifications") {
    val arr = Array[Int]()
    arr += 1
    arr += 2
    arr += 3

    arr.foreach { cur =>
      arr -= cur
    }

    assertEquals(arr.isEmpty, true)
  }

  test("foreach with in-place modifications (2)") {
    val arr = Array[Int]()
    arr += 1

    /* Should not iterate over newly added elements. */
    arr.foreach(arr += _)

    assertEquals(arr.size, 2)
  }

  test("remove") {
    val arr = Array[Int]()
    intercept[AssertionError] {
      arr -= 1
    }
  }
}
