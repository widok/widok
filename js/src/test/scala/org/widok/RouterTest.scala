package org.widok

import minitest._

object RouterTest extends SimpleTestSuite {
  test("compare()") {
    val route = Route("/", null)
    val route2 = Route("/abc", null)
    val route3 = Route("/abc/", null)
    val route4 = Route("/abc//", null)
    val route5 = Route("/:abc", null)
    val route6 = Route("/test/:id", null)
    val route7 = Route("/test2/:id", null)

    assertEquals(route.compare(route), 0)
    assertEquals(route.compare(route2), -1)
    assertEquals(route2.compare(route), 1)
    assertEquals(route2.compare(route3), 0)
    assertEquals(route2.compare(route4), 0)
    assertEquals(route2.compare(route5), 1)
    assertEquals(route5.compare(route2), -1)
    assertEquals(route5.compare(route5), 0)
    assertEquals(route6.compare(route7), -1)
    assertEquals(route7.compare(route6), 1)
  }

  test("query()") {
    val route = Route("/:a/:b", null)
    assertEquals(route(Map("a" -> "/", "b" -> "ß")).uri(), "#/%2F/%C3%9F")
  }

  test("queryParts()") {
    val url = "http://localhost:8080/#/a/http%3A%2F%2Flocalhost%3A8080%2F%23%2Fa"
    assertEquals(Router.queryParts(url), Seq("", "a", "http://localhost:8080/#/a"))

    val url2 = "http://localhost:8080/#/"
    assertEquals(Router.queryParts(url2), Seq())

    val url3 = "http://localhost:8080/#"
    assertEquals(Router.queryParts(url3), Seq())

    val url4 = "http://localhost:8080/"
    assertEquals(Router.queryParts(url4), Seq())
  }
}
