import sbt.Keys._
import sbt._
import Common._

name := "contacts_management"

// lazy val forms = (project in file("../forms"))
// lazy val contacts_management = (project in file(".")) . dependsOn(forms)

libraryDependencies ++= Seq()

publishArtifact in (Compile, packageDoc) := false
sources in (Compile,doc) := Seq.empty

