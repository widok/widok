# Getting Started
## First project
We recommend the use of SBT for compilation as it supports continuous compilation and is the official build system used by Scala.js. If you want to use an IDE, Widok is well-supported by IntelliJ.

Create a new directory ``project`` with two files:

- ``plugins.sbt``

```scala
logLevel := Level.Warn

addSbtPlugin("org.scala-lang.modules.scalajs" % "scalajs-sbt-plugin" % "0.5.5")
```

- ``Build.scala``

```scala
import sbt._
import sbt.Keys._
import scala.scalajs.sbtplugin.ScalaJSPlugin._

object Build extends sbt.Build {
  val projectName = "example"
  val buildOrganisation = "org.widok"
  val buildVersion = "0.1-SNAPSHOT"
  val buildScalaVersion = "2.11.2"
  val buildScalaOptions = Seq(
    "-unchecked", "-deprecation",
    "-encoding", "utf8",
    "-Xelide-below", annotation.elidable.ALL.toString)

  lazy val main = Project(id = projectName, base = file("."))
    .settings(scalaJSSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
        "io.github.widok" %%% "widok" % "0.1.3"
      ),
      organization := buildOrganisation,
      version := buildVersion,
      scalaVersion := buildScalaVersion,
      scalacOptions := buildScalaOptions,
      ScalaJSKeys.persistLauncher := true
    )
}
```

The source code goes underneath ``src/main/scala/org/example``.

Create a source file ``Main.scala`` with the following contents:

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

Finally, you need to create an HTML file ``application.html`` in the root directory which includes the compiled JavaScript sources:

```html
<!DOCTYPE html>
<html>
  <head>
    <meta charset="utf-8">
    <title>Widok example</title>
  </head>
  <body id="page">
    <script type="text/javascript" src="./target/scala-2.11/example-fastopt.js"></script>
    <script type="text/javascript" src="./target/scala-2.11/example-launcher.js"></script>
  </body>
</html>
```

To compile your application application, run:
```bash
$ sbt fastOptJS
```

Now you can open ``application.html`` in your browser.

## Troubleshooting
If sbt does not find the shapeless dependency, try to add the following resolver:

```scala
resolvers += "bintray-alexander_myltsev" at "http://dl.bintray.com/alexander-myltsev/maven/"
```

See also https://github.com/widok/todomvc/issues/1

## Compilation
The latest version is always published to Sonatype and Maven Central. Therefore, no manual compilation of Widok is required. Please refer to [Developing](Developing) if you would like to try out the latest development release.

