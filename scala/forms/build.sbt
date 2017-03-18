import sbt.Keys._
import sbt._
import Common._

name := "semantic_forms"

libraryDependencies ++= commonDependencies

// necessary to set the working directory when running tests from starting SBT in parent directory
fork := true

// javaOptions in test ++= Seq( "-Xdebug" , "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=9999" )
// PENDING: useful? parallelExecution in Test := false


