// import sbt.Keys._
// import sbt._

import Common._
libraryDependencies ++= commonDependencies
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.16"

// necessary to set the working directory when running tests from starting SBT in parent directory
fork := true

publishArtifact in (Compile, packageDoc) := false
sources in (Compile,doc) := Seq.empty

