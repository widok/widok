package org.widok

import scala.collection.mutable
import scala.scalajs.test.JasmineTest

case class Test(a: Int, b: Boolean)

object ChannelTest extends JasmineTest {
  describe("Channel") {
    it("should never be equal to some other Channel") {
      val a = Channel[Int]()
      val b = Channel[Int]()

      expect(a == b).toBe(false)
      expect(a == a).toBe(true)
    }

    it("should be usable as key in HashMap") {
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

    it("should distinct()") {
      val ch = OptVar[Int]()
      val ch2 = ch.distinct

      var sum = 0
      ch2.attach(sum += _.get)

      ch := Some(1)
      ch := Some(1)
      ch := Some(1)

      expect(sum).toBe(1)
    }

    it("should distinct()") {
      val ch = LazyVar[Int](42).distinct

      var sum = 0
      ch.attach(cur => sum += cur)

      expect(sum).toBe(42)
    }

    it("should take()") {
      val ch = Channel[Int]()

      var sum = 0
      ch.take(2).attach(cur => sum += cur)

      expect(ch.children.size).toBe(1)
      ch := 1
      ch := 2

      expect(ch.children.size).toBe(0)
      ch := 3
      ch := 4

      expect(sum).toBe(1 + 2)
    }

    it("should skip()") {
      val ch = Channel[Int]()

      var sum = 0
      ch.skip(2).attach(cur => sum += cur)

      ch := 1
      ch := 2
      ch := 3
      ch := 4

      expect(sum).toBe(3 + 4)
    }

    // TODO
    /*it("should cache()") {
      val ch = Channel[Test]()
      val cache = ch.cache

      val a = cache.value[Int](_ >> 'a)

      var sum = 0
      a.attach(cur => sum += cur)

      ch := Test(1, true)
      expect(sum).toBe(1)

      ch := Test(2, false)
      expect(sum).toBe(1 + 2)

      a := 3
      expect(sum).toBe(1 + 2 + 3)
    }*/

    it("should value()") {
      val ch = Var(Test(1, true))

      val a = ch.value[Int](_ >> 'a)

      var sum = 0
      a.attach(cur => sum += cur)
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

    it("should Var()") {
      val ch = Var(42)
      var sum = 0

      ch.attach(value => sum += value)
      expect(sum).toBe(42)

      ch.attach(value => sum += value + 1)
      expect(sum).toBe(42 + 43)
    }

    it("should +()") {
      val ch = Channel[Int]()

      var sum = 0
      val childCh = ch + (sum += (_: Int))

      ch := 42
      expect(sum).toBe(0)

      childCh := 23
      expect(sum).toBe(23)
    }

    it("should +() with attach()") {
      val ch = Channel[Int]()
      val childCh = ch + ((_: Int) => ())

      var sum = 0
      childCh.attach(sum += _)

      ch := 42
      expect(sum).toBe(42)
    }

    it("should +() with chaining") {
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

    it("should map()") {
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

    it("should zip()") {
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
  }

  describe("LazyVar") {
    it("should apply()") {
      var v = 23
      val ch = LazyVar(v)

      var sum = 0
      ch.attach(value => sum += value)

      expect(sum).toBe(23)

      v = 24
      ch.produce()

      expect(sum).toBe(23 + 24)
    }

    it("should map()") {
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

    it("should filter()") {
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

    it("should take()") {
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

    it("should +()") {
      val ch = LazyVar(42)
      val childCh = ch + ((_: Int) => ())

      var sum = 0
      childCh.attach(sum += _)
      expect(sum).toBe(42)
    }
  }
}