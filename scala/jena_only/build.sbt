// import sbt.Keys._
// import sbt._

scalaVersion in ThisBuild := "2.11.11" // "2.11.8" // "2.12.2"

  val jenaVersion =  "3.3.0"

  val jenaDependency = "org.apache.jena" % "apache-jena-libs" % jenaVersion 
  val jenaTextDependency = "org.apache.jena" % "jena-text" % jenaVersion

publishArtifact in (Compile, packageDoc) := false
sources in (Compile,doc) := Seq.empty

libraryDependencies ++= Seq(
jenaDependency, jenaTextDependency
)

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"
resolvers += Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)

