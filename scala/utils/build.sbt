import sbt.Keys._
import sbt._
import Common._

name := "utils"

// resolvers += bananaResolver

libraryDependencies ++= Seq(
  bananaDependency,
  jenaDependency,
  xmlDependency
)

publishArtifact in (Compile, packageDoc) := false
sources in (Compile,doc) := Seq.empty

