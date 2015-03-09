package org.widok

import minitest._

import scala.collection.mutable

object DictTest extends SimpleTestSuite {
  test("size()") {
    val dict = Dict[Int, Int]()

    var size = -1
    dict.size.attach(size = _)
    assertEquals(size, 0)

    dict += 1 -> 42
    assertEquals(size, 1)
  }

  test("size()") {
    val buf = Buffer(1, 2, 3)
    val map = buf.mapTo(_ => -1)

    var size = 0
    map.size.attach(size = _)
    assertEquals(size, 3)

    buf += 4
    assertEquals(size, 4)
  }

  test("nonEmpty") {
    val dict = Dict[Int, Int]()

    var ne = Option.empty[Boolean]
    dict.nonEmpty.attach(x => ne = Some(x))
    assertEquals(ne, Some(false))

    dict.clear()
    assertEquals(ne, Some(false))
  }

  test("forall()") {
    val buf = Buffer(1, 2, 3)
    val map = buf.mapTo(_ => -1).buffer

    var states = mutable.ArrayBuffer.empty[Boolean]
    map.forall(_ == -1).attach(states += _)
    assertEquals(states, Seq(true))

    map.update(buf.get(0), 1)
    assertEquals(states, Seq(true, false))

    map.update(buf.get(1), 2)
    map.update(buf.get(2), 3)
    map.update(buf.get(0), -1)
    map.update(buf.get(1), -1)
    assertEquals(states, Seq(true, false))

    map.update(buf.get(2), -1)
    assertEquals(states, Seq(true, false, true))
  }

  /* TODO
  test("mapToCh()") {
    val buf = Buffer(0, 1, 2)
    val chs = List(Var("a"), Var("b"), Var("c"))
    val map = buf.mapToCh[String] { value =>
      chs(value).map(str => if (str.nonEmpty) Some(str) else None)
    }

    assertEquals(map.values, Seq("a", "b", "c"))
    chs(1) := ""
    assertEquals(map.values, Seq("a", "c"))
  }
  */

  test("toBuffer") {
    val dict   = Dict(Map(1 -> "one", 2 -> "two", 3 -> "three"))
    val buffer = dict.toBuffer
    assertEquals(buffer.get.sorted, Seq((1, "one"), (2, "two"), (3, "three")))

    dict -= 2
    assertEquals(buffer.get.sorted, Seq((1, "one"), (3, "three")))

    dict.update(1, "forty-two")
    assertEquals(buffer.get.sorted, Seq((1, "forty-two"), (3, "three")))
  }
}
