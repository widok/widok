import sbt._
import sbt.Keys._
import xerial.sbt.Sonatype.SonatypeKeys._
import xerial.sbt.Sonatype.sonatypeSettings
import org.scalajs.sbtplugin._
import org.scalajs.sbtplugin.cross.CrossProject
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import com.typesafe.sbt.web.SbtWeb
import com.typesafe.sbt.web.Import._

object Build extends sbt.Build {
  val buildOrganisation = "io.github.widok"
  val buildScalaVersion = "2.11.6"
  val buildScalaOptions = Seq(
    "-unchecked",
    "-deprecation",
    "-encoding", "utf8"
  )

  def between(subject: String, left: String, right: String): Option[String] = {
    val ofs = subject.indexOf(left)
    val end = subject.indexOf(right, ofs + left.length + 1)
    if (ofs == -1 || end == -1) None
    else Some(subject.slice(ofs + left.length, end))
  }

  def objectName(cssTag: String): String =
    cssTag.foldLeft("") { case (acc, cur) =>
      if (acc.length == 0) "" + cur.toUpper
      else if (acc.last == '-') acc.dropRight(1) + cur.toUpper
      else acc + cur
    }

  def generateFontAwesome(sourceGen: File, webJars: File): Seq[File] = {
    val file = sourceGen / "org" / "widok" / "bindings" / "FontAwesome" / "Icons.scala"

    val objects = io.Source.fromFile(webJars / "lib" / "font-awesome" / "scss" / "_icons.scss")
      .getLines()
      .filter(_.startsWith(".#"))
      .map { line =>
        val icon = between(line, "}-", ":").get
        val obj = objectName(icon)

        s"""
        def $obj() = new Icon { def icon = "$icon" }
        """
      }
      .mkString("\n")

    IO.write(file,
      s"""
      package org.widok.bindings.FontAwesome
      trait Icons {
        $objects
      }
      """
    )

    Seq(file)
  }

  def generateBootstrap(sourceGen: File, webJars: File): Seq[File] = {
    val prefix = "glyphicon"

    val file = sourceGen / "org" / "widok" / "bindings" / "Bootstrap" / "Glyphicon.scala"

    val objects = io.Source.fromFile(webJars / "lib" / "bootstrap" / "less" / "glyphicons.less")
      .getLines()
      .filter(_.startsWith(s".$prefix-"))
      .map { line =>
        val icon =
          between(line, "-", "{")
          .orElse(between(line, "-", ","))
          .get.trim
        val obj = objectName(icon)

        s"""
        def $obj() = new Glyphicon { def icon = "$prefix-$icon" }
         """
      }
      .mkString("\n")

    IO.write(file,
      """
      package org.widok.bindings.Bootstrap

      import org.widok._

      trait Glyphicon extends Widget[Glyphicon] {
        def icon: String
        val rendered = DOM.createElement("span")
        css("glyphicon", icon)
      }
      """ +
      s"""
      object Glyphicon {
        $objects
      }
      """
    )

    Seq(file)
  }

  val codeGenerationTask = {
    val source = sourceManaged in Compile
    val zipped = source.zip(WebKeys.webJarsDirectory in Assets)
    zipped.map { case (src, web) =>
      generateFontAwesome(src, web) ++ generateBootstrap(src, web)
    }
  }.dependsOn(WebKeys.webJars in Assets)

  lazy val root = project.in(file(".")).
    aggregate(js, jvm).
    settings(
      publish := {},
      publishLocal := {}
    )

  lazy val widok = crossProject.in(file("."))
    .enablePlugins(SbtWeb)
    .settings(sonatypeSettings: _*)
    .settings(
      name := "widok",
      version := "0.3.0-SNAPSHOT",

      pomExtra :=
        <url>http://widok.github.io/</url>
        <licenses>
          <license>
            <name>Apache-2.0</name>
            <url>https://www.apache.org/licenses/LICENSE-2.0.html</url>
          </license>
        </licenses>
        <scm>
          <url>git://github.com/widok/widok.git</url>
        </scm>
        <developers>
          <developer>
            <id>tindzk</id>
            <name>Tim Nieradzik</name>
            <url>http://github.com/tindzk/</url>
          </developer>
        </developers>,

      testFrameworks += new TestFramework("minitest.runner.Framework"),

      organization := buildOrganisation,
      scalaVersion := buildScalaVersion,
      scalacOptions := buildScalaOptions,

      autoAPIMappings := true,
      apiMappings += (scalaInstance.value.libraryJar -> url(s"http://www.scala-lang.org/api/${scalaVersion.value}/"))
    )
    .jsSettings(
      sourceGenerators in Compile <+= codeGenerationTask,

      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % "0.8.1",
        "org.monifu" %%% "minitest" % "0.12" % "test",
        "org.webjars" % "font-awesome" % "4.3.0-1",
        "org.webjars" % "bootstrap" % "3.3.2"
      ),

      /* Use io.js for faster compilation of test cases */
      scalaJSStage in Global := FastOptStage
    )
    .jvmSettings(
      libraryDependencies ++= Seq(
        "org.monifu" %% "minitest" % "0.12" % "test"
      )
    )

  lazy val js = widok.js
  lazy val jvm = widok.jvm
}
