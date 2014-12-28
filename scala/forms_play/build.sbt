// import play.Project._

organization := "deductions"

name := "semantic_forms_play"

version := "1.0-SNAPSHOT"

lazy val semantic_forms =  RootProject(file("../forms"))

lazy val semantic_forms_play = (project in file("."))
        .dependsOn(semantic_forms)
	.enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.0" % Test


javacOptions ++= Seq("-source","1.7", "-target","1.7")

// resolvers += Resolver.mavenLocal
resolvers += Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)
// cf http://stackoverflow.com/questions/16400877/local-dependencies-resolved-by-sbt-but-not-by-play-framework

