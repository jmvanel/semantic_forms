import sbt.Keys._
import sbt._
import Common._

name := "sparql_cache"

libraryDependencies ++= Seq(
  bananaDependency
)
libraryDependencies ++=commonDependencies
libraryDependencies += "org.apache.logging.log4j" % "log4j-core" % "2.8.2"