// import sbt.Keys._
// import sbt._
import Common._

name := "html"

// lazy val utils = (project in file("../utils"))
// lazy val utils = RootProject(file("../utils"))
// lazy val connectors = (project in file(".")) .dependsOn(utils)

libraryDependencies ++= loggingDependencies
//Seq(
//   "org.apache.logging.log4j" % "log4j-api" % "2.8",
//    "org.apache.logging.log4j" % "log4j-core" % "2.8" 
//)

publishArtifact in (Compile, packageDoc) := false
sources in (Compile,doc) := Seq.empty

