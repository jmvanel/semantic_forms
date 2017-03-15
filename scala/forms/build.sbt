// import Common._

organization := "deductions"
name := "semantic_forms"
version := "1.0-SNAPSHOT"

scalaVersion :=  "2.11.8" // scalaVersion :=  "2.12.1"
val jenaVersion =  "3.2.0" // "3.1.0"

javacOptions ++= Seq("-source","1.7", "-target","1.7")
scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-explaintypes", "-language:_", "-Xlint:_")

libraryDependencies += "org.w3" %%  "banana-jena" % "0.8.4-SNAPSHOT" // 9.0-SNAPSHOT"

libraryDependencies += "org.apache.jena" % "apache-jena-libs" % jenaVersion  exclude("log4j", "log4j") exclude("org.slf4j", "slf4j-api" ) exclude( "org.apache.logging.log4j" , "log4j-slf4j-impl" )
libraryDependencies += "org.apache.jena" % "jena-permissions" % jenaVersion
libraryDependencies += "org.apache.jena" % "jena-text" % jenaVersion

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.6"
libraryDependencies += "org.apache.lucene" % "lucene-suggest" % "4.9.1"
libraryDependencies += "org.apache.lucene" % "lucene-demo" % "4.9.1"

// libraryDependencies += "net.rootdev" % "java-rdfa" % "0.4.3-SNAPSHOT"
libraryDependencies += "net.rootdev" % "java-rdfa" % "0.4.2"

//libraryDependencies += "com.typesafe.akka" %% "akka-http-core-experimental" % "0.11"
// libraryDependencies += "com.typesafe.akka" %% "akka-http-core-experimental" % "2.0.5"
// See more at: http://search.maven.org/#search%7Cga%7C1%7Cakka-http-core ; https://typesafe.com/blog/akka-http-preview#sthash.IZR1O9fx.dpuf

libraryDependencies += "org.scala-lang.modules" %% "scala-async" % "0.9.6"

//libraryDependencies += "com.typesafe.play" %% "play-json" % "2.3.10"
// 
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.5.12"
// libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.0-M2"

// libraryDependencies += "com.netaporter" %% "scala-i18n" % "0.1"
libraryDependencies += "org.apache.commons" % "commons-csv" % "1.4"
libraryDependencies += "org.apache.any23" % "apache-any23-csvutils" %  "1.1" // "1.0" // 

libraryDependencies ++= Seq(
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.8",
  "org.apache.logging.log4j" % "log4j-api" % "2.8",
  "org.apache.logging.log4j" % "log4j-core" % "2.8"
)

// necessary to set the working directory when running tests from starting SBT in parent directory
fork := true

// javaOptions in test ++= Seq( "-Xdebug" , "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=9999" )

parallelExecution in Test := false

libraryDependencies += "junit" % "junit" % "4.12" % Test
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % Test

// resolvers += "spray repo" at "http://repo.spray.io"
// resolvers += Resolver.url("inthenow-releases", url("http://dl.bintray.com/inthenow/releases"))(Resolver.ivyStylePatterns)

// outdated:  resolvers += "betehess" at  "http://dl.bintray.com/banana-rdf/banana-rdf"

resolvers += "apache-repo-releases" at "https://repository.apache.org/content/repositories/releases/"

