enablePlugins(ScalaJSPlugin)

scalaJSOptimizerOptions ~= { _.withDisableOptimizer(true) }

organization := "deductions"

name := "forms_js"

version := "1.0-SNAPSHOT"

scalaVersion :=  "2.11.7"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

// lazy val root = project.enablePlugins(ScalaJSPlugin)

resolvers += Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)

// TODO banana JS:	libraryDependencies += "org.w3" %%  "banana-jena" % "0.8.2-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.scala-js" %%% "scalajs-dom" % "0.8.2"
)
// libraryDependencies += "org.scala-js" % "scalajs-dom_sjs0.6_2.11" % "0.8.2"

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)

// updateBrowsers <<= updateBrowsers.triggeredBy(fastOptJS in Compile)

