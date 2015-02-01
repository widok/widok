# Concepts
In this chapter we will mention all key concepts of Widok. The following chapters will deal with these topics in detail.

## Continuous compilation
SBT can detect changes in source files. It recompiles the project only when needed:

```bash
$ sbt ~fastOptJS
```

## Single-page applications
The basic application from the [last chapter](GettingStarted) looked like this:

```scala
package org.widok.example

import org.widok._
import org.widok.bindings.HTML

object Main extends PageApplication {
  def contents() = Seq(
    HTML.Heading.Level1("Welcome to Widok!"),
    HTML.Paragraph("This is your first application."))

  def ready() {
    log("Page loaded.")
  }
}
```

This is a single-page application. The two methods ``contents()`` and ``ready()`` must be implemented. ``contents()`` is a list of widgets that are rendered when the page is loaded. Afterwards, ``ready()`` gets called.

## Multi-page applications
While for small applications a single-page approach may be sufficient, you should consider using a router and split the application into multiple pages.

```scala
package org.widok.example

import org.widok._

object Routes {
  val main = Route("/", pages.Main())
  val test = Route("/test/:param", pages.Test())
  val notFound = Route("/404", pages.NotFound())

  val routes = Set(main, test, notFound)
}

object Main extends RoutingApplication(Routes.routes, Routes.notFound)
```

The ``Routes`` objects defines all available routes. A query part may also be a named parameter. If the query parameters need to be validated, this should be done in the page itself.

Create a new file for each page:

- ``pages/Main.scala``

```scala
package org.widok.example.pages

import org.widok._
import org.widok.Widget
import org.widok.bindings.HTML
import org.widok.example.Routes

case class Main() extends Page {
  def contents() = HTML.Anchor("Link to second page").url(Routes.test("param", "first page"))

  def ready(route: InstantiatedRoute) {
    println("Page 'main' loaded.")
  }
}
```

Contrary to single-page applications, ``ready()`` expects a parameter which contains information about the chosen route and its parameters.

A route can be instantiated with parameters (``Routes.ROUTE(params)``). This method is overloaded. If a route has more than one parameter, a map must be passed instead.

``HTML.Anchor()`` is a widget that creates a link. In the above example the target path is set to an instantiated route. This is to be preferred over creating links with hand-written paths. Using instantiated routes ensures during compile-time that no invalid routes are referenced. During runtime, assertions will even check whether the correct parameters were specified.

- ``pages/Test.scala``

```scala
package org.widok.example.pages

import org.widok._

case class Test() extends Page {
  val query = Channel[String]()

  def contents() = Seq("Received parameter: ", query)

  def ready(route: InstantiatedRoute) {
    query := route.args("param")
  }
}
```

We are registering a channel and pass it the current query parameter. A channel can be considered as a stream you can send data to. The data then gets directly multiplexed to the subscribers. It is used as a widget in ``contents()``. This subscribes to the channel and whenever a new value is sent to it, it gets rendered automatically without any caching involved.

Each page is instantiated only once (in ``Routes``). If the user changes the URI parameter, the router detects this and calls ``ready()`` again. This feeds the channel the new parameter. The actual HTML page never gets reloaded.

- ``pages/NotFound.scala``

```scala
package org.widok.example.pages

import org.scalajs.dom
import org.widok._
import org.widok.bindings.HTML
import org.widok.example.Routes

case class NotFound() extends Page {
  def contents() = HTML.Heading.Level1("Page not found")

  def ready(route: InstantiatedRoute) {
    dom.setTimeout(() => Routes.main().go(), 2000)
  }
}
```

In the router this page was set as a fall-back route and is loaded if no other page matches (or is loaded explictly). Here we are showing how to call JavaScript functions using the [DOM bindings](https://github.com/scala-js/scala-js-dom). It calls ``go()`` on the ``main`` route after two seconds which redirects to it.

## Widgets
The method ``contents()`` must return a sequence of widgets. When the page is loaded, Widok looks for the element ``page`` in the DOM and adds all rendered widgets from ``contents()`` to it.

Widok defines a couple of implicits to make your code more concise. For example, if there is only one element you could drop the sequence and write:

```scala
def contents() = HTML.Paragraph("text") // <p>text</p>
```

All widgets that expect children actually expect widgets themselves. Therefore, Widok also provides an implicit to convert strings. As in the above example, most channels are converted as well.

The most notable difference to the traditional approach is that instead of writing HTML code you are dealing with type-safe widgets. Widok provides widget bindings for most HTML tags and even custom bindings for Bootstrap. While it is technically possible to embed HTML code (``HTML.Raw()``) and access the elements using ``getElementById()`` as in JavaScript, this is discouraged as Widok provides better ways to interact with elements.

## Pages
As above, it is advisable to put all pages in a package as to separate them from models and custom widgets.

Pages contain the whole layout. To prevent duplication, custom widgets or partials should be created. This becomes necessary when depending on the user devices different widgets shall be rendered.

For example, Bootstrap splits a page into header, body and footer. You could create a trait ``CustomPage`` that contains all shared elements like header and footer and requires you only to define ``body()`` in the pages.

