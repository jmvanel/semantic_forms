import sbt.Keys._
import sbt._
import Common._

name := "web_tests"

lazy val semantic_forms_play = (project in file("."))
	.enablePlugins(GatlingPlugin)

// fork a new JVM for 'test:run', but not 'run'
// fork in Test := true
// fork := true
// add a JVM option to use when forking a JVM for 'run'
// javaOptions += "-Xmx50M"

// Gatling is an open-source load testing framework based on Scala, Akka and Netty
libraryDependencies += "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.1.7" % Test
libraryDependencies += "io.gatling" % "gatling-test-framework" % "2.1.7" % Test

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
resolvers += Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)

