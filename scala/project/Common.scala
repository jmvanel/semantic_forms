import sbt._
import Keys._

object Common {
  val settings: Seq[Setting[_]] = {
    organization := "deductions"
    version := "1.0-SNAPSHOT"

    scalaVersion := "2.11.8"
    javacOptions ++= Seq("-source","1.7", "-target","1.7")
 }
    // resolvers += Resolver.mavenLocal
    val localResolver = Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)
    val bananaResolver = "betehess" at "http://dl.bintray.com/betehess/banana-rdf"

  val jenaVersion =  "3.2.0"

  val bananaDependency = "org.w3" %%  "banana-jena" % "0.8.4-SNAPSHOT"
  val jenaDependency = "org.apache.jena" % "apache-jena-libs" % jenaVersion  exclude("log4j", "log4j") exclude("org.slf4j", "slf4j-api" ) exclude( "org.apache.logging.log4j" , "log4j-slf4j-impl" )

  val xmlDependency = "org.scala-lang.modules" %% "scala-xml" % "1.0.2"
  val junitDependency = "junit" % "junit" % "4.12" % Test

  val scalatestDependency = "org.scalatest" %% "scalatest" % "2.2.0" % "test"

  // libraryDependencies += 
}
