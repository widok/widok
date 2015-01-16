import sbt._
import sbt.Keys._
import org.typelevel.sbt.Developer
import org.typelevel.sbt.TypelevelPlugin._
import org.scalajs.sbtplugin._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

object Build extends sbt.Build {
  val buildOrganisation = "io.github.widok"
  val buildScalaVersion = "2.11.5"
  val buildScalaOptions = Seq(
    "-unchecked", "-deprecation",
    "-encoding", "utf8")

  lazy val main = Project(id = "widok", base = file("."))
    .enablePlugins(ScalaJSPlugin)
    .settings(typelevelDefaultSettings: _*)
    .settings(
      TypelevelKeys.signArtifacts := true,
      TypelevelKeys.githubDevs += Developer("Tim Nieradzik", "tindzk"),
      TypelevelKeys.githubProject := ("widok", "widok"),
      homepage := Some(url("http://widok.github.io/")),
      licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html")),
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % "0.7.0",
        "org.monifu" %%% "minitest" % "0.10" % "test"
      ),
      testFrameworks += new TestFramework("minitest.runner.Framework"),
      organization := buildOrganisation,
      scalaVersion := buildScalaVersion,
      scalacOptions := buildScalaOptions
    )
}
