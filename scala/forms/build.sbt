organization := "deductions"

name := "semantic_forms"

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.4"

javacOptions ++= Seq("-source","1.7", "-target","1.7")

libraryDependencies += "org.w3" %  "banana-jena_2.10" %  "0.6-SNAPSHOT"

libraryDependencies += "junit" % "junit" % "4.8.1" % "test"

resolvers += Resolver.mavenLocal

resolvers += "betehess" at  "http://dl.bintray.com/betehess/banana-rdf"

