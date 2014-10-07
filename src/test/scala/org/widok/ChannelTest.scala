package org.widok

import scala.collection.mutable
import scala.scalajs.test.JasmineTest

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
      b.attach(() => None)

      expect(map(a)).toBe(1)
      expect(map(b)).toBe(2)
    }

    it("should unique()") {
      val ch = Channel[Int]()
      val ch2 = ch.unique

      var sum = 0
      ch2.attach(cur => sum += cur)

      ch := 1
      ch := 1
      ch := 1

      expect(sum).toBe(1)
    }

    it("should take()") {
      val ch = Channel[Int]()

      var sum = 0
      ch.take(2).attach(cur => sum += cur)

      ch := 1
      ch := 2
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

    case class Test(a: Int, b: Boolean)

    it("should lens()") {
      val ch = Channel[Test]()

      val a = ch.lens(_.a, (subject, value: Int) => subject.copy(a = value))

      var sum = 0
      a.attach(cur => sum += cur)

      var sum2 = 0
      ch.attach(cur => sum2 += cur.a)

      ch := Test(1, false)
      ch := Test(2, true)

      expect(sum).toBe(1 + 2)
      expect(sum2).toBe(1 + 2)

      a := 3

      expect(sum).toBe(1 + 2 + 3)
      expect(sum2).toBe(1 + 2 + 3)
    }

    it("should unit()") {
      val ch = Channel.unit(42)
      var sum = 0

      ch.attach(value => sum += value)
      expect(sum).toBe(0)

      ch.populate()
      expect(sum).toBe(42)

      ch.populate()
      expect(sum).toBe(84)
    }
  }
}