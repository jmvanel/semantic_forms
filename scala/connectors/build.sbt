// import sbt.Keys._
// import sbt._
import Common._

name := "connectors"

libraryDependencies ++= Seq(
    any23Dependencies ,
    "org.apache.commons" % "commons-csv" % "1.5" ,
    bananaDependency,
    jenaDependency
)

publishArtifact in (Compile, packageDoc) := false
sources in (Compile,doc) := Seq.empty
