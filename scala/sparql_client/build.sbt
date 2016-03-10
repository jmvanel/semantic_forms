organization := "deductions"
name := "sparql"
version := "1.1-SNAPSHOT"

scalaVersion := "2.11.8"
javacOptions ++= Seq("-source","1.7", "-target","1.7")


libraryDependencies += "org.w3" %%  "jena" % "0.7.2.radical" 
libraryDependencies += "org.w3" %%  "sesame" %  "0.7.2.radical"
libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.6" % Test

resolvers += Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)

resolvers += "betehess" at  "http://dl.bintray.com/banana-rdf/banana-rdf"

    resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

    resolvers += "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/"

    resolvers += "Sonatype OSS Releases" at "http://oss.sonatype.org/content/repositories/releases/"

    resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots"

scalariformSettings
