// import sbt.Keys._
// import sbt._
import Common._

name := "core"

// lazy val utils = (project in file("../utils"))
// lazy val utils = RootProject(file("../utils"))
// lazy val connectors = (project in file(".")) .dependsOn(utils)

libraryDependencies ++= Seq(
"org.scala-lang.modules" %% "scala-xml" % "1.0.6"
)
