package org.widok

import minitest._

object LazyVarTest extends SimpleTestSuite {
  def expect(a: Any) = new Object { def toBe(b: Any) = assertEquals(a, b) }

  test("apply()") {
    var v = 23
    val ch = LazyVar(v)

    var sum = 0
    ch.attach(value => sum += value)

    expect(sum).toBe(23)

    v = 24
    ch.produce()

    expect(sum).toBe(23 + 24)
  }

  test("map()") {
    var v = 23
    val ch = LazyVar(v)
    val map = ch.map(_ + 1)

    var sum = 0
    map.attach(value => sum += value)
    expect(sum).toBe(24)

    map.map(_ + 2).attach(value => sum += value)
    expect(sum).toBe(24 + 26)

    v = 24
    ch.produce()
    expect(sum).toBe(24 + 26 + 25 + 27)
  }

  test("filter()") {
    val ch = LazyVar(42)
    val filter = ch.filter(_ % 2 == 0)

    var sum = 0
    filter.map(_ * 2).attach(value => sum += value)
    expect(sum).toBe(84)

    ch := 1
    expect(sum).toBe(84)

    ch := 2
    expect(sum).toBe(88)
  }

  test("take()") {
    val ch = LazyVar(42)
    val take = ch.take(2)

    var sum = 0
    take.map(_ + 1).attach(value => sum += value)
    expect(sum).toBe(43)

    ch := 1
    expect(sum).toBe(45)

    ch := 1
    expect(sum).toBe(45)
  }

  /*
  test("+()") {
    val ch = LazyVar(42)
    val childCh = ch + ((_: Int) => ())

    var sum = 0
    childCh.attach(sum += _)
    expect(sum).toBe(42)
  }*/
}
