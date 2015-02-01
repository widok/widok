# Applications
Although Widok provides certain abstractions for the DOM, it is not necessarily restricted to the browser or to displaying widgets. It was conceived with modularity in mind. Its libraries could also be beneficial in console applications or test cases.

## Entry point
Consider you have a one-file project consisting of:

```scala
object Main extends Application {
  def main() {
    stub()
  }
}
```

An object of ``Application`` always defines the entry point of the application. There can only be one entry point.

Apparently, no browser-related functionality was used by the above code. Therefore, it also runs under node.js:

```bash
$ sbt fastOptJS
$ cat target/scala-2.11/*js | node
stub
```

You can also open it in the browser and as expected it will print ``stub`` in the browser console.

## Application providers
To reduce the amount of code needed to get started with Widok, it ships two application providers:

- ``PageApplication``: A single-page application
- ``RoutingApplication``: A multi-page application with routing

### Single-page applications
An example for ``PageApplication`` was already given in the chapter [Getting started](#getting-started). The only difference over to a raw ``Application`` is that you need to implement the two methods ``contents()`` and ``ready()``.

### Multi-page applications
For multi-page applications you can use the default router as the application's entry point. In order to do so, create an object of ``RoutingApplication`` with two arguments: the set of enabled routes and a fall-back route. See the chapter on [Router](routing) for more information.

