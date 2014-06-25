organization := "deductions"

name := "semantic_forms_play"

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.4"

play.Project.playScalaSettings

javacOptions ++= Seq("-source","1.7", "-target","1.7")

libraryDependencies += "deductions" %  "semantic_forms" %  "1.0-SNAPSHOT"


