organization := "deductions"

name := "semantic_forms"

version := "1.0-SNAPSHOT"

//lazy val forms_js =  RootProject(file("../forms_js"))

//lazy val semantic_forms = (project in file("."))
//        .dependsOn(forms_js)

scalaVersion :=  "2.11.7"
// scalaVersion :=  "2.12.0-M3"

javacOptions ++= Seq("-source","1.7", "-target","1.7")

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")


// libraryDependencies += "org.w3" %%  "jena" % "0.7.2.radical" 
libraryDependencies += "org.w3" %%  "banana-jena" % // "0.8.1"
"0.8.2-SNAPSHOT"

libraryDependencies += "org.apache.jena" % "apache-jena-libs" % "2.13.0"
// libraryDependencies += "org.apache.jena" % "apache-jena-libs" % "2.12.1"
libraryDependencies += "org.apache.jena" % "jena-text" % "1.1.2"

libraryDependencies += "com.typesafe.akka" %% "akka-http-core-experimental" % "0.11"
// See more at: https://typesafe.com/blog/akka-http-preview#sthash.IZR1O9fx.dpuf

libraryDependencies += "org.scala-lang.modules" %% "scala-async" % "0.9.2"

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.3.4"

// libraryDependencies += "com.netaporter" %% "scala-i18n" % "0.1"



// for debugging the SBT test suite:
// required for the javaOptions to be passed in (??)
// fork := true

// javaOptions in test ++= Seq( "-Xdebug" , "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=9999" )

parallelExecution in Test := false



libraryDependencies += "junit" % "junit" % "4.8.1" % Test

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.6" % Test
// Gatling is an open-source load testing framework based on Scala, Akka and Netty
libraryDependencies += "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.1.7" % Test

libraryDependencies += "io.gatling" % "gatling-test-framework" % "2.1.7" % Test

// resolvers += "spray repo" at "http://repo.spray.io"
// resolvers += Resolver.mavenLocal

resolvers += Resolver.url("inthenow-releases", url("http://dl.bintray.com/inthenow/releases"))(Resolver.ivyStylePatterns)

// outdated:  resolvers += "betehess" at  "http://dl.bintray.com/banana-rdf/banana-rdf"

// banana-rdf still has some dependencies that are not yet on Maven Central
resolvers += Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

// for i18n resolvers += "bintray" at "http://jcenter.bintray.com"

scalariformSettings
