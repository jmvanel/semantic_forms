// import sbt.Keys._
// import sbt._
import Common._

name := "abstract_syntax"

// lazy val utils = (project in file("../utils"))
// lazy val utils = RootProject(file("../utils"))
// lazy val connectors = (project in file(".")) .dependsOn(utils)

libraryDependencies ++= Seq(
    bananaDependency
)