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

    it("should unique()") {
      val ch = CachedChannel[Int]()
      val ch2 = ch.unique

      var sum = 0
      ch2.attach(cur => sum += cur)

      ch := 1
      ch := 1
      ch := 1

      expect(sum).toBe(1)
    }

    it("should unique()") {
      val ch = StateChannel[Int](42).cache.unique

      var sum = 0
      ch.attach(cur => sum += cur)

      expect(sum).toBe(42)
    }

    it("should take()") {
      val ch = Channel[Int]()

      var sum = 0
      ch.take(2).attach(cur => sum += cur)

      expect(ch.observers.size == 1)
      ch := 1
      ch := 2

      expect(ch.observers.size == 0)
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

    it("should cache()") {
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
    }

    it("should value()") {
      val ch = Channel.unit(Test(1, true)).cache

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

    it("should unit()") {
      val ch = Channel.unit(42)
      var sum = 0

      ch.attach(value => sum += value)
      expect(sum).toBe(42)

      ch.attach(value => sum += value + 1)
      expect(sum).toBe(42 + 43)
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
      val ch = CachedChannel[Int]()
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

  describe("StateChannel") {
    it("should apply()") {
      var v = 23
      val ch = StateChannel(v)

      var sum = 0
      ch.attach(value => sum += value)

      expect(sum).toBe(23)

      v = 24
      ch.produce()

      expect(sum).toBe(23 + 24)
    }

    it("should map()") {
      var v = 23
      val ch = StateChannel(v)
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
  }
}