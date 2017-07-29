// import sbt.Keys._
// import sbt._
import Common._

name := "connectors"

// lazy val utils = (project in file("../utils"))
// lazy val utils = RootProject(file("../utils"))
// lazy val connectors = (project in file(".")) .dependsOn(utils)

libraryDependencies ++= Seq(
    "org.apache.any23" % "apache-any23-csvutils" %  "1.1" ,
    "org.apache.commons" % "commons-csv" % "1.4" ,
    bananaDependency
)

publishArtifact in (Compile, packageDoc) := false
sources in (Compile,doc) := Seq.empty
