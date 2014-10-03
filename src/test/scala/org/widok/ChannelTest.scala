package org.widok

import scala.collection.mutable
import scala.scalajs.test.JasmineTest

object ChannelTest extends JasmineTest {
  describe("Channel") {
    it("should never be equal to some other Channel") {
      val a = Channel[Int]()
      val b = Channel[Int]()

      expect(a == b).toBe(false)
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

  describe("Aggregate") {
    it("should append()") {
      val agg = Aggregate[Int]()
      var sum = 0

      agg.attach(new Aggregate.Observer[Int] {
        def append(cur: Channel[Int]) {
          cur.attach(sum += _)
        }

        def remove(cur: Channel[Int]) {
          sum = 0
        }
      })

      val ch = agg.append(42)
      expect(sum).toBe(42)

      agg.remove(ch)
      expect(sum).toBe(0)
    }

    it("should isEmpty()") {
      val agg = Aggregate[Int]()

      var sets = 0
      var state = false

      val isEmpty = agg.isEmpty

      isEmpty.attach(value => {
        sets += 1
        state = value
      })

      expect(sets).toBe(0)
      expect(state).toBe(false)

      isEmpty.populate()

      expect(sets).toBe(1)
      expect(state).toBe(true)

      val ch = agg.append(42)

      expect(sets).toBe(2)
      expect(state).toBe(false)
    }

    it("should nonEmpty()") {
      val agg = Aggregate[Int]()

      var sets = 0
      var state = false

      val nonEmpty = agg.nonEmpty

      nonEmpty.attach(value => {
        sets += 1
        state = value
      })

      expect(sets).toBe(0)
      expect(state).toBe(false)

      nonEmpty.populate()

      expect(sets).toBe(1)
      expect(state).toBe(false)

      val ch = agg.append(42)

      expect(sets).toBe(2)
      expect(state).toBe(true)
    }

    it("should size()") {
      val agg = Aggregate[Int]()

      var currentSize = -1
      val size = agg.size

      size.attach(value => currentSize = value)

      expect(currentSize).toBe(-1)

      size.populate()

      expect(currentSize).toBe(0)

      val ch = agg.append(42)
      val ch2 = agg.append(43)

      expect(currentSize).toBe(2)

      agg.remove(ch)

      expect(currentSize).toBe(1)

      agg.remove(ch2)

      expect(currentSize).toBe(0)
    }

    it("should filter()") {
      val agg = Aggregate[Int]()

      val filter = agg.filter(_ % 2 == 0)

      var count = 0
      filter.size.attach(value => count = value)

      agg.append(3)
      expect(count).toBe(0)

      val two = agg.append(2)
      expect(count).toBe(1)

      agg.append(4)
      agg.append(5)
      expect(count).toBe(2)

      agg.remove(two)
      expect(count).toBe(1)
    }

    it("should filter() with size() and populate()") {
      val agg = Aggregate[Int]()

      val filter = agg.filter(_ % 2 == 0)

      var count = -1
      val size = filter.size
      size.attach(value => count = value)

      size.populate()

      expect(count).toBe(0)

      agg.append(3)
      expect(count).toBe(0)

      val two = agg.append(2)
      expect(count).toBe(1)

      agg.append(4)
      agg.append(5)
      expect(count).toBe(2)

      agg.remove(two)
      expect(count).toBe(1)
    }

    it("should sum()") {
      val agg = Aggregate[Int]()

      var sum = 0
      agg.sum.attach(value => sum = value)

      val one = agg.append(1)
      expect(sum).toBe(1)

      agg.append(2)
      expect(sum).toBe(3)

      agg.remove(one)
      expect(sum).toBe(2)
    }

    it("should filter() with sum()") {
      val agg = Aggregate[Int]()

      val filter = agg.filter(_ % 2 == 0)

      var sum = -1
      filter.sum.attach(value => sum = value)

      val two = agg.append(2)
      expect(sum).toBe(2)

      two := 4
      expect(sum).toBe(4)
    }

    it("should clear()") {
      val agg = Aggregate[Int]()

      var sum = 0
      agg.sum.attach(value => sum = value)

      agg.append(1)
      agg.append(2)

      expect(sum).toBe(1 + 2)

      agg.clear()

      expect(sum).toBe(0)
    }

    it("should clear() on filtered aggregate without back-propagation") {
      val agg = Aggregate[Int]()

      var size = 0
      agg.size.attach(value => size = value)

      val multTwo = agg.filter(_ % 2 == 0)

      agg.append(1)
      agg.append(2)
      agg.append(3)
      agg.append(4)

      expect(size).toBe(4)

      multTwo.clear()

      expect(size).toBe(4)
    }

    it("should clear() on mapped aggregate without back-propagation") {
      val agg = Aggregate[Int]()

      var size = 0
      agg.size.attach(value => size = value)

      val mapped = agg.map(_ * 2)

      agg.append(1)
      agg.append(2)
      agg.append(3)
      agg.append(4)

      expect(size).toBe(4)

      mapped.clear()

      expect(size).toBe(4)
    }

    it("should map()") {
      val agg = Aggregate[Int]()

      val map = agg.map(_ * 100)

      var sum = 0
      map.sum.attach(value => sum = value)

      val one = agg.append(1)
      expect(sum).toBe(100)

      agg.append(2)
      expect(sum).toBe(300)

      agg.remove(one)
      expect(sum).toBe(200)
    }

    it("should filter() with updating") {
      val agg = Aggregate[Int]()

      var size = 0
      val filter = agg.filter(_ > 1)
      filter.size.attach(value => size = value)

      val zero = agg.append(0)
      val one = agg.append(1)

      expect(size).toBe(0)

      zero := 2
      expect(size).toBe(1)

      one := 2
      expect(size).toBe(2)

      one := 1
      expect(size).toBe(1)

      expect(agg.contains(one)).toBe(true)
      expect(agg.contains(zero)).toBe(true)
    }

    it("should forall()") {
      val agg = Aggregate[Int]()

      var gt1 = false
      val forall = agg.forall(_ > 1)
      forall.attach(value => gt1 = value)

      expect(gt1).toBe(false)
      forall.populate()
      expect(gt1).toBe(true)

      val two = agg.append(2)
      expect(gt1).toBe(true)

      agg.remove(two)
      expect(gt1).toBe(true)

      val zero = agg.append(0)
      expect(gt1).toBe(false)

      zero := 2
      expect(gt1).toBe(true)
    }
  }
}
