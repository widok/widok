# Router
When dealing with applications that consist of more than one page, a routing system becomes inevitable.

The router observes the URL's fragment identifier. For example in ``application.html#/page`` the part after the hash symbol ``/page`` denotes the fragment identifier. A router is initialised with a set of routes which defines all addressable pages. A fallback route may also be specified.

## Interface
The router may be used as follows:

```scala
object Main extends Application {
    val main = Route("/", pages.Main())
    val test = Route("/test/:param", pages.Test())
    val test2 = Route("/test/:param/:param2", pages.Test())
    val notFound = Route("/404", pages.NotFound())

    val routes = Set(main, test, notFound)

    def main() {
        val router = Router(enabled, fallback = Some(notFound))
        router.listen()
    }
}
```

``routes`` denotes the list of enabled routes. It should also contain the ``notFound`` route. Otherwise, this route could not be loaded using ``#/404``.

## Routes
To construct a new route, pass the path and the page object to ``Route()``. Page routes may be overloaded with different paths as above with ``test`` and ``test2``.

A part of a path is the contents separated by a slash. For instance, the ``test`` route above has two parts: ``test`` and ``:param``. A part beginning with a colon is a placeholder. It extracts the respective value from the URL's fragment and binds it to the placeholder name. Note that a placeholder always refers to the whole part.

A route is said *instantiated* when it gets called:

```scala
// Zero parameters
val instMain: InstantiatedRoute = Main.main()

// One parameter
val instTest: InstantiatedRoute = Main.test("param", "value")

// Multiple parameters
val instTest2: InstantiatedRoute = Main.test2(Map(
    "param" -> "value",
    "param2" -> "value2"))

// Change the current page to /test/value
instTest.go()
```

To query the instantiated parameters, access the ``args`` field in the first parameter passed to ``ready()``.

```scala
case class Test() extends Page {
  ...
  def ready(route: InstantiatedRoute) {
    log(route.args("param"))

    // Accessing optional parameters with get()
    // This returns an Option[String]
    log(route.args.get("param2"))
  }
}
```

### Motivation
Due to the simple design the router could be efficiently implemented. The routes allow better reasoning than it would be possible if they supported regular expressions. When the router is constructed, it sorts all routes by their length and checks whether there are no conflicts. Also, the restriction that each parameter must be named makes code more readable when referring to parameters of an instantiated route. If validation of parameters is desired, this must be done in ``ready()``. The advantages of the simple design outweigh its limitations.

## Application provider
As the router defines usually the entry point of an application, Widok provides an application provider that enforces better separation:

```scala
object Routes {
  val main = Route("/", pages.Main())
  ...
  val notFound = Route("/404", pages.NotFound())

  val routes = Set(main, ..., notFound)
}

object Main extends RoutingApplication(Routes.routes, Routes.notFound)
```

This is to be preferred when no further logic should be executed in the entry point prior to setting up the router.

