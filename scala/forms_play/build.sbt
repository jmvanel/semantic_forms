organization := "deductions"

name := "semantic_forms_play"

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.4"

javacOptions ++= Seq("-source","1.7", "-target","1.7")

// resolvers += Resolver.mavenLocal

resolvers += Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)
// cf http://stackoverflow.com/questions/16400877/local-dependencies-resolved-by-sbt-but-not-by-play-framework

libraryDependencies += "deductions" %%  "semantic_forms" %  "1.0-SNAPSHOT"

play.Project.playScalaSettings

