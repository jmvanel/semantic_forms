import sbt._
import Keys._

object Common {
  val settings: Seq[Setting[_]] = {
    organization := "deductions"
    version := "1.0-SNAPSHOT"
    scalaVersion := "2.10.4"
    // scalaVersion := "2.11.1"
    javacOptions ++= Seq("-source","1.7", "-target","1.7")
 }
    // resolvers += Resolver.mavenLocal
    val localResolver = Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)
    val bananaResolver = "betehess" at "http://dl.bintray.com/betehess/banana-rdf"

  val bananaDependency = "org.w3" %%  "banana-jena" %  "0.6"
  val httpDependency = "com.typesafe.akka" %% "akka-http-core-experimental" % "0.10"
  // See more at: https://typesafe.com/blog/akka-http-preview#sthash.IZR1O9fx.dpuf
  // libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.2"
  // really needed ? libraryDependencies += "junit" % "junit" % "4.8.1" % "test"
  val scalatestDependency = "org.scalatest" %% "scalatest" % "2.2.0" % "test"
}
