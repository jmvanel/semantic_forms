organization := "deductions"

name := "semantic_forms"

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.4"
// scalaVersion := "2.11.1"

javacOptions ++= Seq("-source","1.7", "-target","1.7")

resolvers += Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)

resolvers += "spray repo" at "http://repo.spray.io"

// libraryDependencies += "org.w3" %  "banana-jena_2.10" %  "0.6"
libraryDependencies += "org.w3" %%  "banana-jena" %  "0.6"

// libraryDependencies += "org.apache.httpcomponents" % "httpcomponents-client" % "4.3.5"
libraryDependencies += "io.spray" %% "spray-client" % "1.3.2"
// libraryDependencies += "io.spray" %% "spray-client" % "1.1.2"

libraryDependencies += "junit" % "junit" % "4.8.1" % "test"

// libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.0" % "test"
// libraryDependencies += "org.scalatest" % "scalatest_2.10" % "2.2.0" % "test"
libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.0" % "test"

// resolvers += Resolver.mavenLocal

resolvers += "betehess" at  "http://dl.bintray.com/betehess/banana-rdf"

