package org.widok

import minitest._

import scala.collection.mutable

case class Test(a: Int, b: Boolean)

object ChannelTest extends SimpleTestSuite {
  test("should never be equal to some other Channel") {
    val a = Channel[Int]()
    val b = Channel[Int]()

    assertEquals(a == b, false)
    assertEquals(a == a, true)
  }

  test("should be usable as key in HashMap") {
    val map = new mutable.HashMap[Channel[Int], Int]()

    val a = Channel[Int]()
    val b = Channel[Int]()

    map += (a -> 1)
    map += (b -> 2)

    assertEquals(map(a), 1)
    assertEquals(map(b), 2)

    a := 1

    assertEquals(map(a), 1)
    assertEquals(map(b), 2)
  }

  test("head()") {
    var value = 0
    val ch = Var(1)
    val hd = ch.head
    hd.attach(value = _)
    assertEquals(value, 1)
    value = -1
    hd.attach(value = _)
    assertEquals(value, 1)
  }

  test("distinct()") {
    val ch = Var(1)
    val dis = ch.distinct
    ch := 1
    var sum = 0
    dis.attach(sum += _)
    assertEquals(sum, 1)
  }

  test("distinct()") {
    val ch = Opt[Int](0)
    val ch2 = ch.values.distinct

    var sum = 0
    ch2.attach(sum += _)

    ch := 1
    ch := 1
    ch := 2

    assertEquals(sum, 3)
  }

  test("distinct()") {
    val ch = Opt[Int](0)
    val ch2 = ch.values.distinct

    var sum = 0

    ch := 1
    ch := 1
    ch := 2

    ch2.attach(sum += _)
    assertEquals(sum, 2)
  }

  test("distinct()") {
    val ch = LazyVar[Int](42).distinct

    var sum = 0
    ch.attach(sum += _)

    assertEquals(sum, 42)
  }

  test("attach()") {
    val ch = LazyVar[Int](42)
    var sum = 0
    ch.attach(sum += _)
    assertEquals(sum, 42)
  }

  test("take()") {
    val ch = Channel[Int]()

    var items = mutable.ArrayBuffer.empty[Int]
    ch.take(2).attach(items += _)
    assertEquals(ch.children.size, 1)

    ch := 1
    ch := 2

    assertEquals(items, Seq(1, 2))
    assertEquals(ch.children.size, 0)

    ch := 3
    ch := 4

    assertEquals(items, Seq(1, 2))
  }

  test("take()") {
    val ch = Var[Int](0)

    var items = mutable.ArrayBuffer.empty[Int]
    ch.take(2).attach(items += _)

    ch := 1
    ch := 2
    ch := 3

    assertEquals(items, Seq(0, 1))
  }

  test("take()") {
    /* Multiple subscribers */
    val ch = Var[Int](0)

    var items = mutable.ArrayBuffer.empty[Int]
    var items2 = mutable.ArrayBuffer.empty[Int]

    val tk = ch.take(2)
    tk.attach(items += _)

    ch := 1

    tk.attach(items2 += _)

    ch := 2
    ch := 3

    assertEquals(items, Seq(0, 1))
    assertEquals(items2, Seq(1))
  }

  test("take()") {
    val ch  = Var(42)
    val dch = ch.take(2)

    val arr  = mutable.ArrayBuffer.empty[Int]
    val arr2 = mutable.ArrayBuffer.empty[Int]

    dch.attach(arr  += _)
    dch.attach(arr2 += _)

    ch := 23
    ch := 3

    assertEquals(arr,  mutable.ArrayBuffer(42, 23))
    assertEquals(arr2, mutable.ArrayBuffer(42, 23))
  }

  test("drop()") {
    val ch = Channel[Int]()

    var sum = 0
    ch.drop(2).attach(sum += _)

    ch := 1
    ch := 2
    ch := 3
    ch := 4

    assertEquals(sum, 3 + 4)
  }

  test("drop()") {
    val ch = Var(42)
    val dch = ch.drop(1)

    val arr = mutable.ArrayBuffer.empty[Int]
    dch.attach(arr += _)
    dch.attach(arr += _)

    ch := 5

    assertEquals(arr, mutable.ArrayBuffer(5, 5))
  }

  /*test("toOpt()") {
    val ch = Channel[Test]()
    val cache = ch.toOpt

    val a = cache.value[Int](_ >> 'a)

    var sum = 0
    a.attach(sum += _)

    ch := Test(1, true)
    assertEquals(sum, 1)

    ch := Test(2, false)
    assertEquals(sum, 1 + 2)

    a := 3
    assertEquals(sum, 1 + 2 + 3)
  }

  test("value()") {
    val ch = Var(Test(1, true))

    val a = ch.value[Int](_ >> 'a)

    var sum = 0
    a.attach(sum += _)
    assertEquals(sum, 1)

    var sum2 = 0
    ch.attach(cur => sum2 += cur.a)
    assertEquals(sum2, 1)

    ch := Test(2, false)
    ch := Test(3, true)

    assertEquals(sum, 1 + 2 + 3)
    assertEquals(sum2, 1 + 2 + 3)

    a := 4

    assertEquals(sum, 1 + 2 + 3 + 4)
    assertEquals(sum2, 1 + 2 + 3 + 4)
  }*/

  test("Var()") {
    val ch = Var(42)
    var sum = 0

    ch.attach(value => sum += value)
    assertEquals(sum, 42)

    ch.attach(value => sum += value + 1)
    assertEquals(sum, 42 + 43)
  }

  /*
  test("+()") {
    val ch = Channel[Int]()

    var sum = 0
    val childCh = ch + (sum += (_: Int))

    ch := 42
    assertEquals(sum, 0)

    childCh := 23
    assertEquals(sum, 23)
  }

  test("+() with attach()") {
    val ch = Channel[Int]()
    val childCh = ch + ((_: Int) => ())

    var sum = 0
    childCh.attach(sum += _)

    ch := 42
    assertEquals(sum, 42)
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
    assertEquals(chSum, 42)
    assertEquals(chSum2, 0)
    assertEquals(chSum3, 0)

    ch2 := 43
    assertEquals(chSum, 42)
    assertEquals(chSum2, 43)
    assertEquals(chSum3, 0)

    ch3 := 44
    assertEquals(chSum, 42)
    assertEquals(chSum2, 43)
    assertEquals(chSum3, 44)

    childCh := 23
    assertEquals(chSum, 42 + 23)
    assertEquals(chSum2, 43 + 23)
    assertEquals(chSum3, 44 + 23)
  }
  */

  test("map()") {
    val ch = Channel[Int]()
    val map = ch.map(_ + 1)

    var sum = 0
    map.attach(value => sum += value)
    ch := 42
    assertEquals(sum, 43)

    map.attach(value => sum += value + 1)
    ch := 43
    assertEquals(sum, 43 + 44 + 45)
  }

  test("zip()") {
    val ch = Var[Int](0)
    val ch2 = Channel[Int]()

    val zip = ch.zip(ch2)

    var value = (0, 0)
    zip.attach(cur => value = cur)

    ch := 23
    ch2 := 42
    assertEquals(value == (23, 42), true)

    ch := 24
    assertEquals(value == (23, 42), true)

    ch2 := 43
    assertEquals(value == (24, 43), true)
  }

  test("flatMap()") {
    val ch = Channel[Int]()

    val ch2 = Var(0)
    var values = mutable.ArrayBuffer.empty[Int]
    ch2.attach(values += _)

    val fmap = ch.flatMap(_ => ch2)

    var cur = -1
    fmap.attach(cur = _)

    ch := 1
    assertEquals(cur, 0)
    assertEquals(values, mutable.ArrayBuffer(0))

    ch := 2
    ch2 := 2 /* Previous handlers attached to ch2 must be still valid. */
    assertEquals(cur, 2)
    assertEquals(values, mutable.ArrayBuffer(0, 2))
  }

  test("flatMap()") {
    val ch = Channel[Int]()

    val ch2 = Var(2)
    val ch3 = Var(3)

    val fmap = ch.flatMap {
      case 0 => ch2
      case 1 => ch3
    }

    var cur = -1
    fmap.attach(cur = _)

    ch := 0
    assertEquals(cur, 2)
    ch2 := 42
    assertEquals(cur, 42)

    ch := 1
    assertEquals(cur, 3)
    ch3 := 42
    ch2 := 50 /* ch2 should not be attached anymore to flatMap(). */
    assertEquals(cur, 42)
  }

  test("flatMapCh()") {
    val ch = Channel[Var[Int]]()
    val a = ch.flatMapCh(cur => cur)
    val b = ch.flatMapCh(cur => cur)

    ch := Var(42)
  }

  /* test("flatMapCh()") {
    val ch = Channel[Var[Test]]()
    val a = ch.flatMapCh(_.value[Int](_ >> 'a))
    val b = ch.flatMapCh(_.value[Boolean](_ >> 'b))

    var sum = 0
    a.attach(sum += _)
    b.attach(cur => sum += (if (cur) 1 else 0))

    assertEquals(sum, 0)

    val v = Var(Test(2, false))
    ch := v
    assertEquals(sum, 2)

    sum = 0
    v := Test(3, false)
    assertEquals(sum, 3)
  } */

  test("flatMapCh()") {
    val ch = Opt[Int]()
    val ch2 = Channel[Int]()

    val map = ch.flatMapCh(_ => ch2)

    var sum = 0
    ch2.attach(sum += _)

    map := 1
    ch := 0

    map := 5
    assertEquals(sum, 5)
  }

  test("flatMapCh()") {
    val ch = Opt[Int]()
    val map = ch.flatMapCh(_ => Var(42))

    var sum = 0

    ch.values.attach(sum += _)
    map.attach(sum += _)

    ch := 0
    ch := 0

    assertEquals(sum, 84)
  }

  test("writeTo()") {
    val chIn = Channel[Int]()
    val chOut = Channel[Int]()

    var out = -1
    chOut.attach(out = _)

    val ch = chIn.writeTo(chOut)

    chIn := 1
    assertEquals(out, -1)

    ch := 1
    assertEquals(out, 1)
  }

  test("merge()") {
    val ch = Var[Int](5)
    val ch2 = Channel[Int]()

    var out = -1
    ch.merge(ch2).attach(out = _)

    assertEquals(out, 5)

    ch := 1
    assertEquals(out, 1)

    ch2 := 2
    assertEquals(out, 2)
  }

  test("tail()") {
    val ch = Var[Int](42)

    var out = -1
    ch.tail.attach(out = _)

    assertEquals(out, -1)

    ch := 43
    assertEquals(out, 43)
  }

  test("isEmpty()") {
    val ch = Channel[Int]()

    var states = mutable.ArrayBuffer.empty[Boolean]
    ch.isEmpty.attach(states += _)

    ch := 1
    ch := 2

    assertEquals(states, mutable.ArrayBuffer(true, false))
  }

  test("isEmpty()") {
    val ch = Var[Int](2)

    var states = mutable.ArrayBuffer.empty[Boolean]
    ch.isEmpty.attach(states += _)

    ch := 1

    assertEquals(states, mutable.ArrayBuffer(false))
  }

  test("nonEmpty()") {
    val ch = Channel[Int]()

    var states = mutable.ArrayBuffer.empty[Boolean]
    ch.nonEmpty.attach(states += _)

    ch := 1
    ch := 2

    assertEquals(states, mutable.ArrayBuffer(false, true))
  }

  test("nonEmpty()") {
    val ch = Var[Int](2)

    var states = mutable.ArrayBuffer.empty[Boolean]
    ch.nonEmpty.attach(states += _)

    ch := 1

    assertEquals(states, mutable.ArrayBuffer(true))
  }

  test("flatMap()") {
    var value = ""
    Var(42).flatMap(x => Var(x.toString)).filter(_ => true).attach(value = _)
    assertEquals(value, "42")
  }

  test("partialMap()") {
    val ch = Channel[Int]()
    val map = ch.partialMap { case 1 => 42 }

    var states = mutable.ArrayBuffer.empty[Int]
    map.attach(states += _)

    ch := 2
    ch := 3
    ch := 1
    ch := 4

    assertEquals(states, mutable.ArrayBuffer(42))
  }

  test("partialMap()") {
    val ch = Buffer[Int](1, 2, 3).buffer
    val map = ch.changes.partialMap { case Buffer.Delta.Insert(_, 2) => 42 }

    var states = mutable.ArrayBuffer.empty[Int]
    map.attach(states += _)

    assertEquals(states, mutable.ArrayBuffer(42))
  }

  test("Channel()") {
    val rd = Channel[Int]()
    val wr = Channel[Int]()

    val ch = Channel(rd, wr)

    var states = mutable.ArrayBuffer.empty[Int]
    ch.attach(states += _)

    var wrStates = mutable.ArrayBuffer.empty[Int]
    wr.attach(wrStates += _)

    rd := 1
    assertEquals(states, mutable.ArrayBuffer(1))
    assertEquals(wrStates, mutable.ArrayBuffer())

    ch := 2
    assertEquals(states, mutable.ArrayBuffer(1, 2))
    assertEquals(wrStates, mutable.ArrayBuffer(2))
  }
}
