import sbt._
import Keys._

object Common {
    // resolvers += Resolver.mavenLocal
    val localResolver = Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)
    val bananaResolver = "betehess" at "http://dl.bintray.com/betehess/banana-rdf"

//val jenaVersion =  "3.4.0"
  val jenaVersion =  "3.5.0"

  val bananaDependency0 = "org.w3" %%  "banana-jena" % "0.8.4-SNAPSHOT" 
  val bananaDependency = bananaDependency0 exclude("org.slf4j", "slf4j-api" )	exclude("org.slf4j", "slf4j-log4j12")	exclude("org.apache.logging.log4j","log4j-slf4j-impl")
  val jenaDependency = "org.apache.jena" % "apache-jena-libs" % jenaVersion  exclude("log4j", "log4j") exclude("org.slf4j", "slf4j-api" ) exclude( "org.apache.logging.log4j" , "log4j-slf4j-impl" )	exclude("org.slf4j", "slf4j-log4j12")		exclude("org.apache.logging.log4j","log4j-slf4j-impl")
  val jenaPermissionsDependency = "org.apache.jena" % "jena-permissions" % jenaVersion	exclude("org.slf4j", "slf4j-log4j12")	exclude("org.apache.logging.log4j","log4j-slf4j-impl")
  val jenaTextDependency = "org.apache.jena" % "jena-text" % jenaVersion		exclude("org.slf4j", "slf4j-log4j12")	exclude("org.apache.logging.log4j","log4j-slf4j-impl")

  val xmlDependency = "org.scala-lang.modules" %% "scala-xml" % "1.0.6"

  val junitDependency = "junit" % "junit" % "4.12" % Test
  val scalatestDependency = "org.scalatest" %% "scalatest" % "3.0.4" % "test"

  val loggingDependencies = Seq(
    "org.apache.logging.log4j" % "log4j-api" % "2.9.0",
    "org.apache.logging.log4j" % "log4j-core" % "2.9.0",
    "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.9.0" 
  )

  // allready in Banana
  // val httpComponents = "org.apache.httpcomponents" % "httpclient" % "4.5.2"

  val commonDependencies =
    loggingDependencies ++
    Seq( bananaDependency,
	jenaDependency, jenaPermissionsDependency, jenaTextDependency,
	xmlDependency,

	junitDependency, scalatestDependency,

    "com.typesafe.play" %% "play-json" % "2.5.16",

    "org.apache.lucene" % "lucene-suggest" % "6.6.1", // "6.4.1" , // 4.9.1" ,
    "org.apache.lucene" % "lucene-demo" % "6.6.1", // "6.4.1" , // 4.9.1" ,
/*
    "org.apache.lucene" % "lucene-suggest" % "4.9.1" ,
    "org.apache.lucene" % "lucene-demo" % "4.9.1" ,
*/


    // "net.rootdev" % "java-rdfa" % "0.4.3-SNAPSHOT"
    "net.rootdev" % "java-rdfa" % "0.4.2" ,

    //"com.typesafe.akka" %% "akka-http-core-experimental" % "0.11"
    // "com.typesafe.akka" %% "akka-http-core-experimental" % "2.0.5"
    // See more at: http://search.maven.org/#search%7Cga%7C1%7Cakka-http-core ; https://typesafe.com/blog/akka-http-preview#sthash.IZR1O9fx.dpuf

    "org.scala-lang.modules" %% "scala-async" % "0.9.7" ,

    "org.apache.commons" % "commons-csv" % "1.5" ,
    "org.apache.any23" % "apache-any23-csvutils" %  "2.0" // "1.1"
  )

}

