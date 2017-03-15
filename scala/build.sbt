name := "semantic_forms-root"
organization in ThisBuild := "deductions"
version in ThisBuild := "1.9-SNAPSHOT"
scalaVersion in ThisBuild := "2.11.8"

lazy val forms = // (project in file("forms")) // 
project
lazy val forms_play = (project in file("forms_play")) // .dependsOn(forms) .enablePlugins(PlayScala) .disablePlugins(PlayLogback)

lazy val core = project
lazy val generic_app = project

lazy val projects_catalog = project
lazy val contacts_management = project


