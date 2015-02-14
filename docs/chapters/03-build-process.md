# Build process
The chapter [Getting Started](#getting-started) proposed a simple sbt configuration. sbt is a flexible tool and can be extended with plug-ins and custom tasks. Some useful advice on using sbt for web development is given here.

For more information on the build process, please refer to the [Scala.js documentation](http://www.scala-js.org/doc/sbt/run.html).

## Development releases
Code optimisations are time-consuming and usually negligible during development. To compile the project without optimisations, use the ``fastOptJS`` task:

```bash
$ sbt fastOptJS
```

This generates two files in ``target/scala-2.11/``:

- ``$ProjectName-fastopt.js``
- ``$ProjectName-launcher.js``

The former is the whole project including dependencies within a single JavaScript file, while the latter contains a call to the entry point (see also chapter on [applications](#entry-point)).

It would be safe to concatenate these two files and ship them to the client.

## Production releases
Scala.js uses the Google Closure Compiler to apply code optimisations. To create an optimised build, use the ``fullOptJS`` task:

```bash
$ sbt fullOptJS
```

You may want to add a constant to your sbt configuration to toggle compiler settings depending on whether you need a production or development release. For example, ``-Xelidable-below`` could be used to remove assertions from production releases for better performance.

### Additional optimisations
The Scala.js compiler provides settings to fine-tune the build process.

To further reduce the build size, class names could be replaced by an empty string. The semantics of a program should never rely on class names. This optimisation is therefore safe to set. However, if your want to retain some class names, you could define exceptions, for example for classes from a certain namespace.

Another option is to enable unchecked ``asInstanceOf`` casts. A cast should always be well-defined. If this cannot be ensured, a manual ``isInstanceOf`` check needs to be performed. Expecting an exception to be thrown is a suboptimal way of dealing with potentially undefined casts. Under this assumption, ``asInstanceOf`` casts should work if unchecked. Scala.js lets you change the semantics for the sake of better performance.

```scala
import org.scalajs.core.tools.sem._

...

      scalaJSSemantics ~= (_
        .withRuntimeClassName(_ => "")
        .withAsInstanceOfs(CheckedBehavior.Unchecked)
      )
```

Since class names can be useful for debugging purposes and illegal casts may happen during development, these two options should only be set for production releases.

## Continuous compilation
sbt can detect changes in source files and recompile only when needed. To do so, prefix ``~`` to your build task (either ``fastOptJS`` or ``fullOptJS``), for example:

```bash
$ sbt ~fastOptJS
```

This leads to faster development cycles than executing ``fastOptJS`` by your own.

## Configure paths
If the web server should point directly to the most recently built version, you do not need to copy over the generated files each time. Instead, the paths can be customised. A recommended application hierarchy could be the following:

* ``web/index.html``: Self-written entry-point of the application
* ``web/js/``: Generated JavaScript files
* ``web/css/``: Generated CSS stylesheets
* ``web/fonts/``: A copy of all font files (for example, Bootstrap glyphicons or Font-Awesome)

To do so, specify the paths in the build configuration as follows:

```scala
val outPath = new File("web")
val jsPath = outPath / "js"
val cssPath = outPath / "css"
val fontsPath = outPath / "fonts"
```

Scala.js' output path can be remapped using:

```scala
  .settings(
    ...
    artifactPath in (Compile, packageScalaJSLauncher) := jsPath / "launcher.js",
    artifactPath in (Compile, fastOptJS) := jsPath / "application.js"
  )
```

Make sure to also add the following three paths to your ``.gitignore``:

```bash
web/css/
web/js/
web/fonts/
```

## sbt-web
Many popular web libraries are published to Maven Central as regular ``.jar`` files, so called [WebJars](http://www.webjars.org/). See the [official Scala.js documentation](http://www.scala-js.org/doc/sbt/depending.html) on how to depend on these.

[sbt-web](https://github.com/sbt/sbt-web) is an sbt plug-in to manage these WebJars and to produce web artifacts as part of the build process.

To enable ``sbt-web``, add two imports:

```scala
import com.typesafe.sbt.web.SbtWeb
import com.typesafe.sbt.web.Import._
```

And enable the plug-in:

```scala
  .enablePlugins(SbtWeb)
```

For example, to download the [Sass version of the Bootstrap bindings](https://github.com/twbs/bootstrap-sass) as well as Font-Awesome, add these two lines to ``libraryDependencies``:

```scala
libraryDependencies ++= Seq(
  ...
  "org.webjars" % "bootstrap-sass" % "3.3.1",
  "org.webjars" % "font-awesome" % "4.3.0-1"
)
```

> **Note:** sbt-web is not necessary to use Bootstrap or Font-Awesome, albeit it facilitates the customisation and upgrading of web dependencies. The chapter [Bindings](#Bindings) explains how to use a CDN instead.

### Sass
[Sass](http://sass-lang.com/) is a CSS dialect with useful extensions. One of its strengths is that you can modularise your stylesheets and store in separate files. Since Bootstrap is available as Sass, the [sbt-sass plug-in](https://github.com/ShaggyYeti/sbt-sass) for sbt-web lets you create one monolithic, minified CSS file for your whole application. You may find that the widgets Bootstrap provides are not sufficient for your purposes. Using Sass, you would not end up with additional CSS files that need to be included in your ``application.html``, which in turn will increase load times.

Assuming that you want to use Bootstrap and Font-Awesome in your application, create the directory ``src/main/assets/`` with the file ``application.scss`` containing:

```scss
$icon-font-path: "../fonts/";
@import "lib/bootstrap-sass/stylesheets/bootstrap.scss";

$fa-font-path: "../fonts/";
@import "lib/font-awesome/scss/font-awesome.scss";
```

Then, add to your ``plugins.sbt``:

```scala
resolvers += Resolver.url("GitHub repository", url("http://shaggyyeti.github.io/releases"))(Resolver.ivyStylePatterns)

addSbtPlugin("default" % "sbt-sass" % "0.1.9")
```

And configure the output path of the produced CSS files in your ``Build.scala``:

```scala
resourceManaged in sass in Assets := cssPath
```

Finally, add to your ``.gitignore``:

```bash
.sass-cache/
```

sbt-sass requires that the official Sass compiler is installed on your system.

### Font-Awesome
In order to automatically copy the Font-Awesome files to your configured path ``fontsPath``, you can define a sbt-web task:

```scala
val copyFontsTask = {
  val webJars = WebKeys.webJarsDirectory in Assets
  webJars.map { path =>
    val fonts = path / "lib" / "font-awesome" / "fonts"
    fonts.listFiles().map { src =>
      val tgt = fontsPath / src.getName
      IO.copyFile(src, tgt)
      tgt
    }.toSeq
  }
}.dependsOn(WebKeys.webJars in Assets)
```

And register it via:

```scala
sourceGenerators in Assets <+= copyFontsTask
```

### Artifacts
When you issue the sbt task ``assets``, sbt-web will generate your web artifacts, like CSS files.

## Code sharing
Scala.js provides a simple infrastructure to having separate sub-projects for JavaScript and JVM sources, which can share code. This is quite common for client-server applications which could have a common protocol specified in Scala code. You can work on your entire project in the IDE and easily jump between server and client code.

Such projects are called *cross projects* in Scala.js. You can find more information in the [official documentation](http://www.scala-js.org/doc/sbt/cross-building.html).

```scala
import org.scalajs.sbtplugin.cross.CrossProject

object Build extends sbt.Build {
  lazy val crossProject = CrossProject("server", "client", file("."), CrossType.Full)
    .settings(
      /* Shared settings */
    )
    .jvmSettings(
      /* JVM settings */
    )
    .jsSettings(
      /* Scala.js settings */
    )

  lazy val jvm = crossProject.jvm
  lazy val js = crossProject.js
}
```

You will also need to move your current ``src/`` folder to ``js/``. The JVM project goes underneath ``jvm/src/main/scala/`` and the shared source files underneath ``shared/src/main/scala/``.

