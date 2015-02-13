# Build process
Widok uses sbt as its build system.

## Development releases
As with most programming languages, code optimisations are time-consuming and negligible during development. To compile the project without optimisations, run the following sbt command:

```bash
$ sbt fastOptJS
```

Note that prefixing ``~`` makes sbt constantly check for changed source files and only recompiles when needed.

This generates two files in ``target/scala-2.11/``:

- ``APPNAME-fastopt.js``
- ``APPNAME-launcher.js``

The former contains the whole project including its dependencies within a single JavaScript file, while the latter is a call to the entry point (see also chapter on [applications](#entry-point)).

It is safe to concatenate these two files and ship them to the client.

## Production releases
Scala.js uses the Google Closure Compiler to apply code optimisations. To create an optimised build, run:

```bash
$ sbt fullOptJS
```

Similarly as with ``fastOptJS``, you can also prefix ``~`` here.

> **TODO:** Explain how to maintain sbt setups for production and development releases. This is necessary in order to use a different value for ``-Xelidable-below``.

## Configure paths
If the web server should point directly to the latest built version, you do not need to copy over the generated files each time. Instead, the paths can be customised. A recommended application hierarchy could be the following:

* ``web/index.html``: Self-written entry-point of the application
* ``web/js/``: Generated JavaScript files
* ``web/css/``: Generated CSS stylesheets
* ``web/fonts/``: Copy of the fonts

Specify the paths in the build configuration as follows:

```scala
val outPath = new File("web")
val jsPath = outPath / "js"
val cssPath = outPath / "css"
val fontsPath = outPath / "fonts"
```

The Scala.js' output path can then be configured using:

```scala
artifactPath in (Compile, packageScalaJSLauncher) := jsPath / "launcher.js",
artifactPath in (Compile, fastOptJS) := jsPath / "application.js",
```

Make sure to add the following three paths to ``.gitignore``:

```bash
web/css/
web/js/
web/fonts/
```

## sbt-web
Many popular web libraries are published to Maven Central as regular ``.jar`` files, so called [WebJars](http://www.webjars.org/). See the [official Scala.js documentation](http://www.scala-js.org/doc/sbt/depending.html) on how to depend on these.

[sbt-web](https://github.com/sbt/sbt-web) is an sbt plugin to manage these WebJars and to produce web artifacts as part of the build process.

For example, to download the SASS version of the Bootstrap bindings as well as Font-Awesome, just write:

```scala
libraryDependencies ++= Seq(
  ...
  "org.webjars" % "bootstrap-sass" % "3.3.1",
  "org.webjars" % "font-awesome" % "4.3.0-1"
),
```

When you issue the sbt command ``assets``, sbt-web will create its artifacts.

sbt-web is not necessary to use Bootstrap or Font-Awesome, albeit facilitates their customisation. The chapter [Bindings](#Bindings) explains how to use a CDN instead.

### Sass
[Sass](http://sass-lang.com/) is a CSS dialect with useful extensions. One of its strengths is that you can modularise your stylesheets and include files. Since Bootstrap is available as Sass, the [sbt-sass plug-in](https://github.com/ShaggyYeti/sbt-sass) for sbt-web lets you create one monolithic, minified CSS file for your whole website. You may find that the widgets Bootstrap provides are not sufficient for your purposes, so you would end up with additional stylesheet rules specified in external CSS files, which in turn increases load times.

In ``src/main/assets/`` of your website create the file ``application.scss`` containing:

```scss
$icon-font-path: "../fonts/";
@import "lib/bootstrap-sass/stylesheets/bootstrap.scss";

$fa-font-path: "../fonts/";
@import "lib/font-awesome/scss/font-awesome.scss";
```

This assumes that your application is using the Bootstrap and Font-Awesome bindings.

Then, add to your ``plugins.sbt``:

```scala
resolvers += Resolver.url("GitHub repository", url("http://shaggyyeti.github.io/releases"))(Resolver.ivyStylePatterns)
addSbtPlugin("default" % "sbt-sass" % "0.1.9")
```

And configure the output path of the produced CSS files in your ``Build.scala``:

```scala
resourceManaged in sass in Assets := cssPath
```

Add to your ``.gitignore``:

```bash
.sass-cache/
```

### Font-Awesome
In order to copy the Font-Awesome files to your configured path ``fontsPath``, you can define a sbt-web task:

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

## Links
For more information on the build process, please refer to the [Scala.js manual](http://www.scala-js.org/doc/sbt/run.html).

