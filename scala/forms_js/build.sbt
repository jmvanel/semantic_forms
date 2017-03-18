import sbt.Keys._
import sbt._
import Common._

enablePlugins(ScalaJSPlugin)
scalaJSOptimizerOptions ~= { _.withDisableOptimizer(true) }

name := "forms_js"

// lazy val root = project.enablePlugins(ScalaJSPlugin)
// TODO banana JS:	libraryDependencies += "org.w3" %%  "banana-jena" % "0.8.2-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.scala-js" %%% "scalajs-dom" % "0.9.0"
)

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"
resolvers += Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)
// updateBrowsers <<= updateBrowsers.triggeredBy(fastOptJS in Compile)

