// import sbt.Keys._
// import sbt._

import Common._
libraryDependencies ++= commonDependencies
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.6.5" // 5.30"

// https://mvnrepository.com/artifact/org.locationtech.jts/jts
//libraryDependencies += "org.locationtech.jts" % "jts" % "1.16.1" pomOnly()

// NOTE: fork := true necessary to set the working directory when running tests from starting SBT in parent directory
fork := false
parallelExecution in Test := false

// cf http://www.scalatest.org/user_guide/using_scalatest_with_sbt
logBuffered in Test := false

publishArtifact in (Compile, packageDoc) := false
sources in (Compile,doc) := Seq.empty


// NOTE: required for the javaOptions to be passed in
// fork := true

// javaOptions in (Test) += "-Xdebug"
// javaOptions in (Test) += "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=9999"

