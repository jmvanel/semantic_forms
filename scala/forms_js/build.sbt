import sbt.Keys._
import sbt._
// import Common._ // TODO

scalaVersion in ThisBuild := "2.11.11" // "2.11.8" // "2.12.2"

enablePlugins(ScalaJSPlugin)
scalaJSOptimizerOptions ~= { _.withDisableOptimizer(true) }

name := "forms_js"

libraryDependencies ++= Seq(
  "org.scala-js" %%% "scalajs-dom" % "0.9.2" ,
  "org.w3" %%  "banana-plantain" % "0.8.4-SNAPSHOT"
)

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"
resolvers += Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)
// updateBrowsers <<= updateBrowsers.triggeredBy(fastOptJS in Compile)

