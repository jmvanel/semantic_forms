import sbt.Keys._
import sbt._
import Common._

name := "sparql_cache"

libraryDependencies ++= Seq(
  bananaDependency
)
libraryDependencies ++=commonDependencies

publishArtifact in (Compile, packageDoc) := false
sources in (Compile,doc) := Seq.empty

