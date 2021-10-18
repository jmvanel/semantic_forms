// import sbt.Keys._
// import sbt._

import Common._
libraryDependencies ++= commonDependencies
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % akkaVersion

// https://mvnrepository.com/artifact/org.locationtech.jts/jts
//libraryDependencies += "org.locationtech.jts" % "jts" % "1.16.1" pomOnly()

// NOTE: fork := true necessary to set the working directory when running tests from starting SBT in parent directory
fork := false
Test / parallelExecution := false

// cf http://www.scalatest.org/user_guide/using_scalatest_with_sbt
Test / logBuffered := false

// NOTE: required for the javaOptions to be passed in ?
// fork := true

// javaOptions in (Test) += "-Xdebug"
// javaOptions in (Test) += "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=9999"

