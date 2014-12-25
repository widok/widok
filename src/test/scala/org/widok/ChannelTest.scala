package org.widok

import cgta.otest.FunSuite
import scala.collection.mutable

case class Test(a: Int, b: Boolean)

object ChannelTest extends FunSuite {
  def expect(a: Any) = new Object { def toBe(b: Any) = Assert.isEquals(a, b) }

  test("should never be equal to some other Channel") {
    val a = Channel[Int]()
    val b = Channel[Int]()

    expect(a == b).toBe(false)
    expect(a == a).toBe(true)
  }

  test("should be usable as key in HashMap") {
    val map = new mutable.HashMap[Channel[Int], Int]()

    val a = Channel[Int]()
    val b = Channel[Int]()

    map += (a -> 1)
    map += (b -> 2)

    expect(map(a)).toBe(1)
    expect(map(b)).toBe(2)

    a := 1

    expect(map(a)).toBe(1)
    expect(map(b)).toBe(2)
  }

  test("distinct()") {
    val ch = Var(1)
    val dis = ch.distinct
    ch := 1
    var sum = 0
    dis.attach(sum += _)
    expect(sum).toBe(1)
  }

  test("distinct()") {
    val ch = Opt[Int](0)
    val ch2 = ch.distinct

    var sum = 0
    ch2.attach(sum += _)

    ch := 1
    ch := 1
    ch := 2

    expect(sum).toBe(3)
  }

  test("distinct()") {
    val ch = Opt[Int](0)
    val ch2 = ch.distinct

    var sum = 0

    ch := 1
    ch := 1
    ch := 2

    ch2.attach(sum += _)
    expect(sum).toBe(2)
  }

  test("distinct()") {
    val ch = LazyVar[Int](42).distinct

    var sum = 0
    ch.attach(sum += _)

    expect(sum).toBe(42)
  }

  test("take()") {
    val ch = Channel[Int]()

    var items = mutable.ArrayBuffer.empty[Int]
    ch.take(2).attach(items += _)
    expect(ch.children.size).toBe(1)

    ch := 1
    ch := 2

    expect(items).toBe(Seq(1, 2))
    expect(ch.children.size).toBe(0)

    ch := 3
    ch := 4

    expect(items).toBe(Seq(1, 2))
  }

  test("take()") {
    val ch = Var[Int](0)

    var items = mutable.ArrayBuffer.empty[Int]
    ch.take(2).attach(items += _)

    ch := 1
    ch := 2
    ch := 3

    expect(items).toBe(Seq(0, 1))
  }

  test("skip()") {
    val ch = Channel[Int]()

    var sum = 0
    ch.skip(2).attach(sum += _)

    ch := 1
    ch := 2
    ch := 3
    ch := 4

    expect(sum).toBe(3 + 4)
  }

  test("toOpt()") {
    val ch = Channel[Test]()
    val cache = ch.toOpt

    val a = cache.value[Int](_ >> 'a)

    var sum = 0
    a.attach(sum += _)

    ch := Test(1, true)
    expect(sum).toBe(1)

    ch := Test(2, false)
    expect(sum).toBe(1 + 2)

    a := 3
    expect(sum).toBe(1 + 2 + 3)
  }

  test("value()") {
    val ch = Var(Test(1, true))

    val a = ch.value[Int](_ >> 'a)

    var sum = 0
    a.attach(sum += _)
    expect(sum).toBe(1)

    var sum2 = 0
    ch.attach(cur => sum2 += cur.a)
    expect(sum2).toBe(1)

    ch := Test(2, false)
    ch := Test(3, true)

    expect(sum).toBe(1 + 2 + 3)
    expect(sum2).toBe(1 + 2 + 3)

    a := 4

    expect(sum).toBe(1 + 2 + 3 + 4)
    expect(sum2).toBe(1 + 2 + 3 + 4)
  }

  test("Var()") {
    val ch = Var(42)
    var sum = 0

    ch.attach(value => sum += value)
    expect(sum).toBe(42)

    ch.attach(value => sum += value + 1)
    expect(sum).toBe(42 + 43)
  }

  test("+()") {
    val ch = Channel[Int]()

    var sum = 0
    val childCh = ch + (sum += (_: Int))

    ch := 42
    expect(sum).toBe(0)

    childCh := 23
    expect(sum).toBe(23)
  }

  test("+() with attach()") {
    val ch = Channel[Int]()
    val childCh = ch + ((_: Int) => ())

    var sum = 0
    childCh.attach(sum += _)

    ch := 42
    expect(sum).toBe(42)
  }

  test("+() with chaining") {
    val ch = Channel[Int]()
    var chSum = 0
    ch.attach(chSum += _)

    val ch2 = Channel[Int]()
    var chSum2 = 0
    ch2.attach(chSum2 += _)

    val ch3 = Channel[Int]()
    var chSum3 = 0
    ch3.attach(chSum3 += _)

    val childCh = ch + ch2 + ch3

    ch := 42
    expect(chSum).toBe(42)
    expect(chSum2).toBe(0)
    expect(chSum3).toBe(0)

    ch2 := 43
    expect(chSum).toBe(42)
    expect(chSum2).toBe(43)
    expect(chSum3).toBe(0)

    ch3 := 44
    expect(chSum).toBe(42)
    expect(chSum2).toBe(43)
    expect(chSum3).toBe(44)

    childCh := 23
    expect(chSum).toBe(42 + 23)
    expect(chSum2).toBe(43 + 23)
    expect(chSum3).toBe(44 + 23)
  }

  test("map()") {
    val ch = Channel[Int]()
    val map = ch.map(_ + 1)

    var sum = 0
    map.attach(value => sum += value)
    ch := 42
    expect(sum).toBe(43)

    map.attach(value => sum += value + 1)
    ch := 43
    expect(sum).toBe(43 + 44 + 45)
  }

  test("zip()") {
    val ch = Var[Int](0)
    val ch2 = Channel[Int]()

    val zip = ch.zip(ch2)

    var value = (0, 0)
    zip.attach(cur => value = cur)

    ch := 23
    ch2 := 42
    expect(value == (23, 42)).toBe(true)

    ch := 24
    expect(value == (23, 42)).toBe(true)

    ch2 := 43
    expect(value == (24, 43)).toBe(true)
  }

  test("flatMapCh()") {
    val ch = Channel[Var[Int]]()
    val a = ch.flatMapCh(cur => cur)
    val b = ch.flatMapCh(cur => cur)

    ch := Var(42)
  }

  test("flatMapCh()") {
    val ch = Channel[Var[Test]]()
    val a = ch.flatMapCh(_.value[Int](_ >> 'a))
    val b = ch.flatMapCh(_.value[Boolean](_ >> 'b))

    var sum = 0
    a.attach(sum += _)
    b.attach(cur => sum += (if (cur) 1 else 0))

    expect(sum).toBe(0)

    val v = Var(Test(2, false))
    ch := v
    expect(sum).toBe(2)

    sum = 0
    v := Test(3, false)
    expect(sum).toBe(3)
  }

  test("flatMapCh()") {
    val ch = Opt[Int]()
    val ch2 = Channel[Int]()

    val map = ch.flatMapCh(_ => ch2)

    var sum = 0
    ch2.attach(sum += _)

    map := 1
    ch := 0

    map := 5
    Assert.isEquals(sum, 5)
  }

  test("flatMapCh()") {
    val ch = Opt[Int]()
    val map = ch.flatMapCh(_ => Var(42))

    var sum = 0

    ch.attach(sum += _)
    map.attach(sum += _)

    ch := 0
    ch := 0

    Assert.isEquals(sum, 84)
  }

  test("writeTo()") {
    val chIn = Channel[Int]()
    val chOut = Channel[Int]()

    var out = -1
    chOut.attach(out = _)

    val ch = chIn.writeTo(chOut)

    chIn := 1
    Assert.isEquals(out, -1)

    ch := 1
    Assert.isEquals(out, 1)
  }

  test("merge()") {
    val ch = Var[Int](5)
    val ch2 = Channel[Int]()

    var out = -1
    ch.merge(ch2).attach(out = _)

    Assert.isEquals(out, 5)

    ch := 1
    Assert.isEquals(out, 1)

    ch2 := 2
    Assert.isEquals(out, 2)
  }

  test("tail()") {
    val ch = Var[Int](42)

    var out = -1
    ch.tail.attach(out = _)

    Assert.isEquals(out, -1)

    ch := 43
    Assert.isEquals(out, 43)
  }
}
