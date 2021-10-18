// import sbt.Keys._
// import sbt._
import Common._

name := "core"

libraryDependencies ++= Seq(
  xmlDependency,
  scalazDependency
)
libraryDependencies ++= loggingDependencies

