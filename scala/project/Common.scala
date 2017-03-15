import sbt._
import Keys._

object Common {
    // resolvers += Resolver.mavenLocal
    val localResolver = Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)
    val bananaResolver = "betehess" at "http://dl.bintray.com/betehess/banana-rdf"

  val jenaVersion =  "3.2.0"

  val bananaDependency = "org.w3" %%  "banana-jena" % "0.8.4-SNAPSHOT"
  val jenaDependency = "org.apache.jena" % "apache-jena-libs" % jenaVersion  exclude("log4j", "log4j") exclude("org.slf4j", "slf4j-api" ) exclude( "org.apache.logging.log4j" , "log4j-slf4j-impl" )
  val jenaPermissionsDependency = "org.apache.jena" % "jena-permissions" % jenaVersion
  val jenaTextDependency = "org.apache.jena" % "jena-text" % jenaVersion

  val xmlDependency = "org.scala-lang.modules" %% "scala-xml" % "1.0.6"

  val junitDependency = "junit" % "junit" % "4.12" % Test
  val scalatestDependency = "org.scalatest" %% "scalatest" % "3.0.1" % "test"

  libraryDependencies := Seq( bananaDependency,
	jenaDependency, jenaPermissionsDependency, jenaTextDependency,
	xmlDependency,
	junitDependency, scalatestDependency  )

libraryDependencies += "org.apache.lucene" % "lucene-suggest" % "4.9.1"
libraryDependencies += "org.apache.lucene" % "lucene-demo" % "4.9.1"

// libraryDependencies += "net.rootdev" % "java-rdfa" % "0.4.3-SNAPSHOT"
libraryDependencies += "net.rootdev" % "java-rdfa" % "0.4.2"

//libraryDependencies += "com.typesafe.akka" %% "akka-http-core-experimental" % "0.11"
// libraryDependencies += "com.typesafe.akka" %% "akka-http-core-experimental" % "2.0.5"
// See more at: http://search.maven.org/#search%7Cga%7C1%7Cakka-http-core ; https://typesafe.com/blog/akka-http-preview#sthash.IZR1O9fx.dpuf

libraryDependencies += "org.scala-lang.modules" %% "scala-async" % "0.9.6"

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.5.12"
// libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.0-M2"

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

}

