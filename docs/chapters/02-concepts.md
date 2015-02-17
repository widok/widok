# Concepts
In this chapter we will mention all key concepts of Widok. The following chapters will deal with these topics in detail.

## Basic application
Consider a one-file project consisting of:

```scala
object Main extends Application {
  def main() {
    stub()
  }
}
```

A global object of type ``Application`` defines the entry point of the application^[An application cannot define more than one entry point.]. You could use methods from the ``DOM`` object to access and modify the DOM.

```scala
$ sbt fastOptJS
```

Open your ``application.html`` in the browser and it will print ``stub`` in the web console. The example doesn't use any browser-related functionality. Therefore, it also runs under [Node.js](http://nodejs.org/):

```bash
$ cat target/scala-2.11/*js | node
stub
```

## Single-page applications
The application from the [previous chapter](#getting-started) roughly looked like this:

```scala
package example

import org.widok._

object Main extends PageApplication {
  def view() = Inline()
  def ready() { }
}
```

For a single-page application you need to declare an object which inherits from ``PageApplication``, whereby Scala.js knows that it shall be the entry-point of the program.

Furthermore, the two methods ``view()`` and ``ready()`` must be implemented. The views are rendered when the page is loaded. Afterwards, ``ready()`` gets called.

In the example, the ``Inline`` view is used which in contrast to a ``<span>`` or ``<div>`` cannot be controlled using a stylesheet rule.

## Multi-page applications
While for small applications a single-page approach may be sufficient, you should consider to make use of the in-built router and split your application into multiple pages for better modularity:

```scala
package example

import org.widok._

object Routes {
  val main = Route("/", pages.Main)
  val test = Route("/test/:param", pages.Test)
  val notFound = Route("/404", pages.NotFound)

  val routes = Set(main, test, notFound)
}

object Main extends RoutingApplication(Routes.routes, Routes.notFound)
```

A multi-page application must extend from ``RoutingApplication`` which is passed a list of routes and a fallback route. Here, the ``Routes`` objects defines the available routes. The query part of a route can be parameterised by prefixing a colon. For instance, ``param`` is a named parameter for the route ``test``. The router has a simple design and matches arbitrary strings. Further validations and conversions can be performed in the page itself.

Create a new file for each page:

- ``pages/Main.scala``

```scala
package example.pages

import org.widok._
import org.widok.Widget
import org.widok.bindings.HTML

import example._

case class Main() extends Page {
  def view() = HTML.Anchor("Link to second page")
    .url(Routes.test("param", "first page"))

  def ready(route: InstantiatedRoute) {
    println(s"Page 'main' loaded with route '$route'")
  }

  override def destroy() {
    println("Page 'main' left")
  }
}
```

Contrary to single-page applications, ``ready()`` needs one parameter which contains information about the chosen route and its parameters.

This page uses ``HTML.Anchor()`` which is a widget representing a link. The target URL is set to an instantiated route, namely ``test``. Every route can be instantiated, although all parameters as in the routes specification must be provided. The ``apply()`` method of a route is overloaded. For only one route parameter, the first argument denotes the named parameter and the second its value. If a route has more than one parameter, a map with the values must be passed instead. Instantiating routes is to be preferred over to creating links with hand-written paths. Referencing routes ensures during compile-time that no invalid routes are referenced. During runtime, assertions will verify whether all correct parameters were specified.

When clicking the link, the router will notice this change and render the new route. The actual HTML file of the page is not reloaded, though.

By default, ``destroy()`` is a stub, but may be overridden when navigating between routes requires resource management.

- ``pages/Test.scala``

```scala
package example.pages

import org.widok._

case class Test() extends Page {
  val query = Channel[String]()
  def view() = Inline("Received parameter: ", query)

  def ready(route: InstantiatedRoute) {
    query := route.args("param")
  }
}
```

Here, we are registering a channel and pass it the current value of the query parameter. A channel can be considered as a stream you can send data to. The data then gets directly multiplexed to the subscribers. ``query`` has one subscriber here. As it is used in ``view()``, it is converted into a view. Whenever a new value is produced on ``query`` (using ``:=``), it gets rendered automatically in the browser. If the user changes the query parameter of the current page, the router will detect this and re-render the page.

- ``pages/NotFound.scala``

```scala
package example.pages

import org.scalajs.dom

import org.widok._
import org.widok.bindings.HTML

import example.Routes

case class NotFound() extends Page {
  def view() = HTML.Heading.Level1("Page not found")

  def ready(route: InstantiatedRoute) {
    dom.setTimeout(() => Routes.main().go(), 2000)
  }
}
```

``NotFound`` was set as a fall-back route. It is loaded when no other route matches or when the fall-back route is loaded explicitly. Here, we are showing how to call JavaScript functions using the [DOM bindings](https://github.com/scala-js/scala-js-dom). It redirects to the ``main`` page after two seconds by calling ``go()`` on the instantiated route.

## Pages and Widgets
As in the multi-page application, it is advisable to put all pages in a package as to separate them from other parts of the application, like models, validators or partials^[Partials are composed widgets.].

When the page is loaded, Widok looks for the element with the ID ``page`` in the DOM and renders ``view()`` in it. The entire contents is destroyed when the route changes.

``view()`` must return the whole layout of the page. To prevent duplication among pages, partials should be defined. Common candidates for partials are navigation bars, panels or layout elements. But as partials are just regular functions returning a widget, they can contain logic and you may render different widgets depending on whether the user accesses the website on a mobile device or on a desktop.

For instance, Bootstrap splits pages into header, body and footer. You could create a trait ``CustomPage`` that contains all shared elements like header and footer and requires you only to define ``body()`` in the pages.

The most notable difference to the traditional approach is that instead of writing HTML code you are dealing with type-safe widgets. Widok provides widget bindings for HTML tags and custom bindings for Bootstrap as well as Font-Awesome. It is possible to embed HTML code using the ``HTML.Raw()`` widget. You could even access DOM elements using ``DOM.getElementById()`` as in JavaScript. However, this is discouraged in Widok which provides better ways to interact with elements.

## Reactive programming
To showcase some of the capabilities of reactive programming for UI development, take the following example:

```scala
package example

import org.widok._
import org.widok.html._

object App extends PageApplication {
  val name = Var("")
  val hasName = name.map(_.nonEmpty)

  def view() = div(
    h1("Welcome to Widok!")
  , p("Please enter your name:")

  , text().bind(name)

  , p("Hello, ", name)
      .show(hasName)

  , button("Change my name")
      .onClick(_ => name := "tux")
      .show(name.unequal("tux"))

  , button("Log out")
      .onClick(_ => name := "")
      .show(hasName)
  )

  def ready() { }
}
```

The first striking change from the previous examples is that we now use the HTML aliases (``import org.widok.html._``).

More importantly, this example shows that widgets provide methods to interact with channels. For example, the method ``bind()`` on textual input fields realises two-way binding, i.e., every key stroke produces a new value on the channel and notifies all other subscribers.

Another such method is ``show()`` which will only show a certain widget if the passed channel produces the value ``true``.

``Var()`` is a channel with an empty value as a default value and is bound to ``name``. Well-known combinators such as ``map()`` and ``filter()`` are also defined on channels. In the example, ``map()`` is used for ``hasName`` such that the channel notifies its subscribers whenever ``name`` is updated.

