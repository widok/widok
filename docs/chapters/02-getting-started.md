# Getting Started
This chapter will guide you through creating your first Widok project.

## Prerequisites
To develop web applications with Widok the only dependency you will need is [sbt](http://www.scala-sbt.org/). Once installed, it will automatically fetch Scala.js and all libraries Widok depends on.

You may also want to use an IDE for development. Widok is well-supported by [IntelliJ IDEA](https://www.jetbrains.com/idea/) with the [Scala plugin](https://github.com/JetBrains/intellij-scala). The use of an IDE is recommended as the interfaces Widok provides are fully typed, which lets you do tab completion.

## Project structure
Your project will have the following structure:

```
├── application.html
├── project
│   ├── Build.scala
│   ├── plugins.sbt
├── src
│   └── main
│       └── scala
│           └── example
│               └── Application.scala
```

Create a directory for your project. Within your project folder, create a sub-directory ``project`` with the two files ``plugins.sbt`` and ``Build.scala``:

- ``plugins.sbt`` specifies sbt plug-ins, notably Scala.js

```scala
logLevel := Level.Warn

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.0")
```

- ``Build.scala`` is the build configuration of your project. The configuration itself is specified in Scala code, which allows for more flexibility. The chapter ['Build process'](#build-process) explains some of the possibilities in the web context.

```scala
import sbt._
import sbt.Keys._
import org.scalajs.sbtplugin._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

object Build extends sbt.Build {
  val buildOrganisation = "example"
  val buildVersion = "0.1-SNAPSHOT"
  val buildScalaVersion = "2.11.5"
  val buildScalaOptions = Seq(
    "-unchecked", "-deprecation"
  , "-encoding", "utf8"
  , "-Xelide-below", annotation.elidable.ALL.toString
  )

  lazy val main = Project(id = "example", base = file("."))
    .enablePlugins(ScalaJSPlugin)
    .settings(
      libraryDependencies ++= Seq(
        "io.github.widok" %%% "widok" % "0.2.0"
      )
    , organization := buildOrganisation
    , version := buildVersion
    , scalaVersion := buildScalaVersion
    , scalacOptions := buildScalaOptions
    , persistLauncher := true
    )
}
```

Your source code goes underneath ``src/main/scala/example/``.

## Code
Create a source file named ``Main.scala`` with the following contents:

```scala
package example

import org.widok._
import org.widok.bindings.HTML

object Main extends PageApplication {
  def view() = Inline(
    HTML.Heading.Level1("Welcome to Widok!")
  , HTML.Paragraph("This is your first application.")
  )

  def ready() {
    log("Page loaded.")
  }
}
```

Finally, you need to create an HTML file ``application.html`` in the root directory. It references the compiled JavaScript sources:

```html
<!DOCTYPE html>
<html>
  <head>
    <meta charset="utf-8">
    <title>Widok example</title>
  </head>
  <body id="page">
    <script
      type="text/javascript"
      src="./target/scala-2.11/example-fastopt.js"
    ></script>

    <script
      type="text/javascript"
      src="./target/scala-2.11/example-launcher.js"
    ></script>
  </body>
</html>
```

## Compilation
This is all you need for a minimal Widok project. To compile your application, run:

```bash
$ sbt fastOptJS
```

Now you can open ``application.html`` in your browser. The page should show a heading with a paragraph. Obviously, the Scala code you wrote translates to:

```html
<h1>Welcome to Widok!</h1>
<p>This is your first application.</p>
```

Upon page load this gets dynamically inserted into the node with the ID ``page``. When you open up the browser's web console, it will show the message you specified in ``ready()``.

