import sbt.Keys._
import sbt._

scalaVersion := // "2.12.4" //
		"2.11.12"

val scalawebtestVersion = // "2.0.2-SNAPSHOT"
			"2.0.1"

libraryDependencies += "org.scalawebtest" %% "scalawebtest-core" % scalawebtestVersion % Test // "it"
// libraryDependencies += "org.scalawebtest" %% "scalawebtest-integration" % scalawebtestVersion

// cf http://www.scala-sbt.org/0.13/docs/Testing.html
lazy val scalawebtest = (project in file("."))
  .configs(IntegrationTest)

