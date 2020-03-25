// import sbt.Keys._
// import sbt._

import Common._
libraryDependencies ++= commonDependencies
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.17"

// https://mvnrepository.com/artifact/org.locationtech.jts/jts
//libraryDependencies += "org.locationtech.jts" % "jts" % "1.16.1" pomOnly()

// necessary to set the working directory when running tests from starting SBT in parent directory
fork := true

publishArtifact in (Compile, packageDoc) := false
sources in (Compile,doc) := Seq.empty

//required for the javaOptions to be passed in
// fork := true

// javaOptions in (Test) += "-Xdebug"
// javaOptions in (Test) += "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=9999"

