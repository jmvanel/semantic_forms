import sbt.Keys._
import sbt._
// import Common._ // TODO

// scalaVersion := "2.11.12" // "2.12.6" //
// scalaVersion := "2.12.7"

enablePlugins(ScalaJSPlugin)
// scalaJSOptimizerOptions ~= { _.withDisableOptimizer(true) }

name := "forms_js"

publishArtifact in (Compile, packageDoc) := false
sources in (Compile,doc) := Seq.empty

libraryDependencies ++= Seq(
  // "org.scala-js" %%% "scalajs-dom" % "0.9.5" ,
  "org.scala-js" %%% "scalajs-dom" % "0.9.6" ,
  "org.w3" %%  "banana-plantain" % "0.8.4-SNAPSHOT"
)

// libraryDependencies += "be.doeraene" %%% "scalajs-jquery" % "0.9.3"
libraryDependencies += "be.doeraene" %%% "scalajs-jquery" % "0.9.4"

// resolvers += Resolver.jcenterRepo
// libraryDependencies += "com.definitelyscala" %%% "scala-js-jqueryui" % "1.0.2"
// Classes and traits are available in the package com.definitelyscala.jqueryui, scaladoc is provided.

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"
resolvers += Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)
// updateBrowsers <<= updateBrowsers.triggeredBy(fastOptJS in Compile)


// From https://github.com/vmunier/play-with-scalajs-example

  // scalaJSUseMainModuleInitializer := true
  // enablePlugins(ScalaJSPlugin, ScalaJSWeb)
