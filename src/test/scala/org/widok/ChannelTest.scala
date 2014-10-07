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

    it("should filter() with back-propagation of added elements") {
      val agg = Aggregate[Int]()

      val filter = agg.filter(_ % 2 == 0)

      var count = 0
      filter.size.attach(value => count = value)

      val three = agg.append(3)
      val four = agg.append(4)
      val five = agg.append(5)
      val six = agg.append(6)

      filter.clear()

      expect(count).toBe(0)
      expect(agg.contains(three)).toBe(true)
      expect(agg.contains(four)).toBe(false)
      expect(agg.contains(five)).toBe(true)
      expect(agg.contains(six)).toBe(false)

      val ten = filter.append(10)
      expect(agg.contains(ten)).toBe(true)
    }

    it("should filter() with back-propagation of changed elements") {
      val agg = Aggregate[Int]()

      val filter = agg.filter(_ % 2 == 0)
      val cache = filter.cache

      var sum = 0
      agg.sum.attach(value => sum = value)

      agg.append(4)
      expect(sum).toBe(4)

      filter.append(3)
      cache.update(_ + 2)

      expect(sum).toBe(4 + 5)
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

      one := 2
      expect(sum).toBe(200)

      agg.append(2)
      expect(sum).toBe(400)

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

    it("should update()") {
      val agg = Aggregate[Int]()

      var appends = 0
      var removes = 0

      agg.attach(new Aggregate.Observer[Int] {
        def append(cur: Channel[Int]) {
          appends += 1
        }

        def remove(cur: Channel[Int]) {
          removes += 1
        }
      })

      val ch = agg.append(1)
      val ch2 = agg.append(2)
      val ch3 = agg.append(3)

      val chCache = ch.cache
      val chCache2 = ch2.cache
      val chCache3 = ch3.cache

      expect(appends).toBe(3)

      chCache.update(_ * 100)
      chCache2.update(_ * 100)
      chCache3.update(_ * 100)

      expect(appends).toBe(3)
      expect(removes).toBe(0)

      agg.remove(ch)
      expect(removes).toBe(1)
    }

    it("should sum()") {
      val agg = Aggregate[Int]()

      var sum = 0
      agg.sum.attach(value => sum = value)

      val ch = agg.append()
      val ch2 = agg.append()
      val ch3 = agg.append()

      val chCache = ch.cache
      val chCache2 = ch2.cache
      val chCache3 = ch3.cache

      ch := 1
      ch2 := 2
      ch3 := 3

      expect(sum).toBe(1 + 2 + 3)

      chCache.update(_ * 100)
      expect(chCache.get.get).toBe(100)

      chCache2.update(_ * 100)
      expect(chCache2.get.get).toBe(200)

      chCache3.update(_ * 100)
      expect(chCache3.get.get).toBe(300)

      expect(sum).toBe(100 + 200 + 300)

      agg.remove(ch)
      expect(sum).toBe(200 + 300)
    }
  }

  describe("CachedAggregate") {
    it("should update()") {
      val agg = Aggregate[Int]()
      val cache = agg.cache

      var sum = 0
      agg.sum.attach(value => sum = value)

      val ch = agg.append(1)
      val ch2 = agg.append(2)
      val ch3 = agg.append(3)

      expect(sum).toBe(1 + 2 + 3)

      cache.update(_ * 10)
      cache.update(_ * 10)

      expect(sum).toBe(100 + 200 + 300)

      agg.remove(ch)
      expect(sum).toBe(200 + 300)
    }

    it("should update() with filter()") {
      val agg = Aggregate[Int]()
      val cache = agg.cache

      var sum = 0
      agg.filter(_ > 1).sum.attach(value => sum = value)

      val ch = agg.append(1)
      val ch2 = agg.append(2)
      val ch3 = agg.append(3)

      expect(sum).toBe(2 + 3)

      cache.update(_ * 10)
      cache.update(_ * 10)

      expect(sum).toBe(100 + 200 + 300)

      agg.remove(ch)
      expect(sum).toBe(200 + 300)
    }

    it("should filterCh()") {
      val agg = Aggregate[Int]()
      val cache = agg.cache

      val filter = Channel[Int => Boolean]()
      val agg2 = cache.filterCh(filter)

      var sum = 0
      agg2.sum.attach(value => sum = value)

      expect(sum).toBe(0)

      filter := (_ > 1)

      val ch = agg.append(1)
      val ch2 = agg.append(2)
      val ch3 = agg.append(3)

      expect(sum).toBe(2 + 3)

      agg.remove(ch2)
      expect(sum).toBe(3)
    }

    it("should filterCh() already existing items") {
      val agg = Aggregate[Int]()
      val cache = agg.cache

      val filter = Channel[Int => Boolean]()
      val agg2 = cache.filterCh(filter)

      var sum = 0
      agg2.sum.attach(value => sum = value)

      val ch = agg.append(1)
      val ch2 = agg.append(2)
      val ch3 = agg.append(3)

      expect(sum).toBe(0)

      filter := (_ > 0)
      expect(sum).toBe(1 + 2 + 3)

      filter := (_ > 1)
      expect(sum).toBe(2 + 3)

      expect(agg.contains(ch)).toBe(true)
      ch := 10
      expect(agg.contains(ch)).toBe(true)

      ch2 := 20
      ch3 := 30

      expect(sum).toBe(10 + 20 + 30)

      agg.remove(ch)
      expect(sum).toBe(20 + 30)
    }

    it("should filterCh() already existing items with update()") {
      val agg = Aggregate[Int]()
      val cache = agg.cache

      val filter = Channel[Int => Boolean]()
      val agg2 = cache.filterCh(filter)

      var sum = 0
      agg2.sum.attach(value => sum = value)

      val ch = agg.append(1)
      val ch2 = agg.append(2)
      val ch3 = agg.append(3)

      expect(sum).toBe(0)

      filter := (_ > 0)
      expect(sum).toBe(1 + 2 + 3)

      filter := (_ > 1)
      expect(sum).toBe(2 + 3)

      cache.update(_ * 10)
      expect(sum).toBe(10 + 20 + 30)

      agg.remove(ch)
      expect(sum).toBe(20 + 30)
    }

    it("should filterCh() with back-propagation of added elements") {
      val agg = Aggregate[Int]()
      val cache = agg.cache

      val filter = Channel[Int => Boolean]()
      val agg2 = cache.filterCh(filter)

      var size = 0
      agg.size.attach(value => size = value)

      val ch = agg.append(1)
      expect(size).toBe(1)

      val ch2 = agg2.append(1)
      expect(size).toBe(2)

      agg2.remove(ch2)
      expect(size).toBe(1)
    }

    it("should filterCh() with back-propagation of changed elements") {
      val agg = Aggregate[Int]()
      val cache = agg.cache

      val filter = Channel[Int => Boolean]()
      val agg2 = cache.filterCh(filter)
      val cache2 = agg2.cache

      var sum = 0
      agg.sum.attach(value => sum = value)

      filter := (_ > 0)

      val ch = agg.append(1)
      val ch2 = agg.append(2)
      val ch3 = agg.append(3)

      expect(sum).toBe(1 + 2 + 3)

      cache2.update(_ + 1)
      expect(sum).toBe(2 + 3 + 4)
      cache2.update(_ - 1)

      filter := (_ > 1)
      cache2.update(_ + 1)
      expect(sum).toBe(1 + 3 + 4)
    }
  }
}