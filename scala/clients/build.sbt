// import sbt.Keys._
// import sbt._
import Common._

name := "clients"

libraryDependencies ++= Seq(
  bananaDependency
)
// libraryDependencies += httpComponents // allready in Banana

publishArtifact in (Compile, packageDoc) := false
sources in (Compile,doc) := Seq.empty

