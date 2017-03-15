name := "semantic_forms-root"

organization in ThisBuild := "deductions"
version in ThisBuild := "1.0-SNAPSHOT" // "1.10-SNAPSHOT"
scalaVersion in ThisBuild := "2.11.8" // scalaVersion :=  "2.12.1"
javacOptions ++= Seq("-source","1.7", "-target","1.7")
scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-explaintypes", "-language:_", "-Xlint:_")

lazy val forms = project // (project in file("forms")) // 
lazy val forms_play = (project in file("forms_play")) // .dependsOn(forms) .enablePlugins(PlayScala) .disablePlugins(PlayLogback)

lazy val core = project
lazy val generic_app = project

lazy val projects_catalog = project
lazy val contacts_management = project

// Scala code checkers

// wartremoverErrors ++= Warts.unsafe
// wartremoverErrors ++= Warts.allBut(Wart.DefaultArguments, Wart.Var)
// libraryDependencies += "com.lightbend" %% "abide-core" % "0.1-SNAPSHOT" % "abide"


