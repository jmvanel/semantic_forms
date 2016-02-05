organization := "deductions"

name := "web_tests"

version := "1.0-SNAPSHOT"

lazy val semantic_forms_play = (project in file("."))
	.enablePlugins(GatlingPlugin)

scalaVersion := "2.11.7"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.6" % Test

// Gatling is an open-source load testing framework based on Scala, Akka and Netty
libraryDependencies += "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.1.7" % Test

libraryDependencies += "io.gatling" % "gatling-test-framework" % "2.1.7" % Test

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

javacOptions ++= Seq("-source","1.7", "-target","1.7")

resolvers += Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)

