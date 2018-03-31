// import sbt.Keys._
// import sbt._
import Common._

name := "utils"

libraryDependencies ++= Seq(
  bananaDependency,
  jenaDependency
)
libraryDependencies += xmlDependency

publishArtifact in (Compile, packageDoc) := false
sources in (Compile,doc) := Seq.empty

