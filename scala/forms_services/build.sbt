import sbt.Keys._
import sbt._
import Common._

name := "semantic_forms_services"

// lazy val semantic_forms =  RootProject(file("../forms"))
// lazy val semantic_forms_services = (project in file(".")) .dependsOn(semantic_forms) .enablePlugins(PlayScala)

resolvers += Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)
// cf http://stackoverflow.com/questions/16400877/local-dependencies-resolved-by-sbt-but-not-by-play-framework


