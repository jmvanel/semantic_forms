import sbt.Keys._
import sbt._
import Common._

// scalaVersion := "2.11.12" // "2.12.6" //
// scalaVersion := "2.12.7"

enablePlugins(ScalaJSPlugin)
// scalaJSOptimizerOptions ~= { _.withDisableOptimizer(true) }

name := "forms_js"

Compile / publishArtifact := false
packageDoc / publishArtifact := false
Compile / sources := Seq.empty
doc / sources := Seq.empty

libraryDependencies ++= Seq(
  // "org.scala-js" %%% "scalajs-dom" % "0.9.6" ,
  "org.scala-js" %%% "scalajs-dom" % "1.2.0" ,
  bananaOrganisation %%  "banana-plantain" % bananaVersion
  // "org.w3" %%  "banana-plantain" % "0.8.4-SNAPSHOT"
  // , "org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.0.0"
)

// libraryDependencies += "be.doeraene" %%% "scalajs-jquery" % "0.9.4"
libraryDependencies += "be.doeraene" %%% "scalajs-jquery" % "1.0.0"

// resolvers += Resolver.jcenterRepo
// libraryDependencies += "com.definitelyscala" %%% "scala-js-jqueryui" % "1.0.2"
// Classes and traits are available in the package com.definitelyscala.jqueryui, scaladoc is provided.

resolvers += "Typesafe Repo" at "https://repo.typesafe.com/typesafe/releases/"
resolvers += Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)
// updateBrowsers <<= updateBrowsers.triggeredBy(fastOptJS in Compile)


// From https://github.com/vmunier/play-with-scalajs-example

  // scalaJSUseMainModuleInitializer := true
  // enablePlugins(ScalaJSPlugin, ScalaJSWeb)

// Add support for the DOM in `run` and `test`
// jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv()

