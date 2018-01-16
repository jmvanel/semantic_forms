// import sbt.Keys._
// import sbt._
import Common._

name := "utils"

libraryDependencies ++= Seq(
  bananaDependency,
  jenaDependency
)
libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.6"

publishArtifact in (Compile, packageDoc) := false
sources in (Compile,doc) := Seq.empty

