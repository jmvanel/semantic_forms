organization := "deductions"

name := "semantic_forms"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.1"

javacOptions ++= Seq("-source","1.7", "-target","1.7")


libraryDependencies += "org.w3" %%  "jena" %  "0.7.1" // "0.7.2-SNAPSHOT"

libraryDependencies += "com.typesafe.akka" %% "akka-http-core-experimental" % "0.10"
// See more at: https://typesafe.com/blog/akka-http-preview#sthash.IZR1O9fx.dpuf

libraryDependencies += "org.scala-lang.modules" %% "scala-async" % "0.9.2"

// libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.2"

libraryDependencies += "junit" % "junit" % "4.8.1" % Test

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.0" % Test



resolvers += Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)

// resolvers += "spray repo" at "http://repo.spray.io"

// resolvers += Resolver.mavenLocal

resolvers += "betehess" at  "http://dl.bintray.com/betehess/banana-rdf"

scalariformSettings
