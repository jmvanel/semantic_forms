// import sbt.Keys._
// import sbt._

// name := "semantic_forms"

lazy val forms = project.in(file("."))
	.dependsOn(utils  % "compile->compile" )
	.aggregate(utils)
lazy val utils = project.in(file("../utils"))
// lazy val utils = project.in(file("utils"))

import Common._
libraryDependencies ++= commonDependencies

// necessary to set the working directory when running tests from starting SBT in parent directory
fork := true

