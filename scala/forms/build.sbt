// import sbt.Keys._
// import sbt._

import Common._
libraryDependencies ++= commonDependencies

// necessary to set the working directory when running tests from starting SBT in parent directory
fork := true

