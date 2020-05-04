import sbt._
import Keys._

object Common {
    // resolvers += Resolver.mavenLocal
    val localResolver = Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)
    val bananaResolver = "bblfish-snapshots" at "http://bblfish.net/work/repo/releases"

//val jenaVersion =  "3.9.0"
//val jenaVersion =  "3.10.0"
// ==>  val jenaVersion =  "3.11.0"
//val jenaVersion =  "3.12.0"
//val jenaVersion =  "3.13.0"
// val jenaVersion =  "3.13.1"
// 
val jenaVersion =  "3.14.0"

  val bananaDependency0 = "org.w3" %%  "banana-jena" % "0.8.4-SNAPSHOT" 
  val bananaDependency = bananaDependency0 exclude("org.slf4j", "slf4j-api" )	exclude("org.slf4j", "slf4j-log4j12")	exclude("org.apache.logging.log4j","log4j-slf4j-impl")
  val jenaDependency = "org.apache.jena" % "apache-jena-libs" % jenaVersion  exclude("log4j", "log4j") exclude("org.slf4j", "slf4j-api" ) exclude( "org.apache.logging.log4j" , "log4j-slf4j-impl" )	exclude("org.slf4j", "slf4j-log4j12")		exclude("org.apache.logging.log4j","log4j-slf4j-impl")
  val jenaPermissionsDependency = "org.apache.jena" % "jena-permissions" % jenaVersion	exclude("org.slf4j", "slf4j-log4j12")	exclude("org.apache.logging.log4j","log4j-slf4j-impl")
  val jenaTextDependency = "org.apache.jena" % "jena-text" % jenaVersion		exclude("org.slf4j", "slf4j-log4j12")	exclude("org.apache.logging.log4j","log4j-slf4j-impl")
  val jenaSpatialDependency = "org.apache.jena" % "jena-spatial" % jenaVersion		exclude("org.slf4j", "slf4j-log4j12")	exclude("org.apache.logging.log4j","log4j-slf4j-impl")


  val xmlDependency = "org.scala-lang.modules" %% "scala-xml" % "1.3.0"

  val junitDependency = "junit" % "junit" % "4.13" % Test
  libraryDependencies += "org.scalactic" %% "scalactic" % "3.1.1"
  val scalatestDependency = "org.scalatest" %% "scalatest" % "3.1.1" % "test"
  logBuffered in Test := false

  val log4jVersion = "2.13.1" // "2.11.2" // "2.11.1"
  val loggingDependencies = Seq(
//    "org.apache.logging.log4j" % "log4j-api" % log4jVersion,
//    "org.apache.logging.log4j" % "log4j-core" % log4jVersion,
//    "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4jVersion
//, "org.apache.logging.log4j" % "log4j-web" % log4jVersion
   "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
,  "org.slf4j" % "slf4j-simple" % "1.7.30"
)

  // val scalazDependency = "org.scalaz" %% "scalaz-core" % "7.2.8"
  val scalazDependency = "org.scalaz" %% "scalaz-core" % "7.2.26"

  // allready in Banana
  // val httpComponents = "org.apache.httpcomponents" % "httpclient" % "4.5.2"

  val any23Dependencies = "org.apache.any23" % "apache-any23-csvutils" % "2.3"

  val commonDependencies =
    loggingDependencies ++
    Seq(bananaDependency,
	jenaDependency, jenaPermissionsDependency, jenaTextDependency,
// jenaSpatialDependency,
	xmlDependency,

	junitDependency, scalatestDependency,

    "com.typesafe.play" %% "play-json" %  "2.8.1" , // 2.6.14",
    // "com.typesafe.play" %% "play-iteratees" % "2.6.1",

    "org.apache.lucene" % "lucene-suggest" % "7.4.0" , // "6.4.1" , // "7.5.0", // 6.6.5",
    "org.apache.lucene" % "lucene-demo"    % "7.4.0" , // "6.4.1" , // "7.5.0", // 6.6.5",
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

    "org.apache.commons" % "commons-csv" % "1.8" ,
    any23Dependencies
    // for Java 9 (works also with Java 8)
    , "javax.xml.bind" % "jaxb-api" % "2.3.1"
  )

}

