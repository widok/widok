import sbt.Keys._
import sbt._
import scala.scalajs.sbtplugin.ScalaJSPlugin._

object Build extends sbt.Build {
  val buildOrganisation = "org.widok"
  val buildVersion = "0.1-SNAPSHOT"
  val buildScalaVersion = "2.11.2"
  val buildScalaOptions = Seq("-unchecked", "-deprecation", "-encoding", "utf8")

  lazy val main = Project(id = "widok", base = file("."))
    .settings(scalaJSSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
        "org.scala-lang.modules.scalajs" %%% "scalajs-dom" % "0.6",
        "org.scala-lang.modules.scalajs" %% "scalajs-jasmine-test-framework" % scalaJSVersion % "test",
        "org.monifu" %% "monifu-js" % "0.14.0.M5"),
      organization := buildOrganisation,
      version := buildVersion,
      scalaVersion := buildScalaVersion,
      scalacOptions := buildScalaOptions,
      ScalaJSKeys.persistLauncher := true)
}