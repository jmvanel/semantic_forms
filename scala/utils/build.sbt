// import sbt.Keys._
// import sbt._
import Common._

name := "utils"

// lazy val utils = project.in(file("."))

libraryDependencies ++= Seq(
  bananaDependency
)

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.6"
