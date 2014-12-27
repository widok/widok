package org.widok

import minitest._

object RouterTest extends SimpleTestSuite {
  def expect(a: Any) = new Object { def toBe(b: Any) = assertEquals(a, b) }

  test("compare()") {
    val route = Route("/", null)
    val route2 = Route("/abc", null)
    val route3 = Route("/abc/", null)
    val route4 = Route("/abc//", null)
    val route5 = Route("/:abc", null)
    val route6 = Route("/test/:id", null)
    val route7 = Route("/test2/:id", null)

    expect(route.compare(route)).toBe(0)
    expect(route.compare(route2)).toBe(-1)
    expect(route2.compare(route)).toBe(1)
    expect(route2.compare(route3)).toBe(0)
    expect(route2.compare(route4)).toBe(0)
    expect(route2.compare(route5)).toBe(1)
    expect(route5.compare(route2)).toBe(-1)
    expect(route5.compare(route5)).toBe(0)
    expect(route6.compare(route7)).toBe(-1)
    expect(route7.compare(route6)).toBe(1)
  }
}
