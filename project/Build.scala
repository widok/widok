import sbt._
import sbt.Keys._
import scala.scalajs.sbtplugin.ScalaJSPlugin._
import org.typelevel.sbt.Developer
import org.typelevel.sbt.TypelevelPlugin._

object Build extends sbt.Build {
  val buildOrganisation = "io.github.widok"
  val buildScalaVersion = "2.11.2"
  val buildScalaOptions = Seq(
    "-unchecked", "-deprecation",
    "-encoding", "utf8")

  lazy val main = Project(id = "widok", base = file("."))
    .settings(scalaJSSettings: _*)
    .settings(typelevelDefaultSettings: _*)
    .settings(cgta.otest.OtestPlugin.settingsSjs: _*)
    .settings(
      TypelevelKeys.signArtifacts := true,
      TypelevelKeys.githubDevs += Developer("Tim Nieradzik", "tindzk"),
      TypelevelKeys.githubProject := ("widok", "widok"),
      homepage := Some(url("http://widok.github.io/")),
      licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html")),
      resolvers += "bintray-alexander_myltsev" at "http://dl.bintray.com/content/alexander-myltsev/maven",
      libraryDependencies ++= Seq(
        "org.scala-lang.modules.scalajs" %%% "scalajs-dom" % "0.6",
        "name.myltsev" %% "shapeless_sjs0.5" % "2.0.0",
        "biz.cgta" %%% "otest-sjs" % "0.1.14" % "test"
      ),
      organization := buildOrganisation,
      scalaVersion := buildScalaVersion,
      scalacOptions := buildScalaOptions,
      ScalaJSKeys.persistLauncher := true
    )
}
