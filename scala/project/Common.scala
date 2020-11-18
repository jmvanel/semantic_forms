import sbt._
import Keys._

object Common {
    // resolvers += Resolver.mavenLocal
    val localResolver = Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)
    val bananaResolver = "bblfish-snapshots" at "http://bblfish.net/work/repo/releases"

val jenaVersion =  "3.16.0"
val akkaVersion =  "2.5.26" // because of Play 2.6.25
                  // "2.6.5" //
val akkaHttpVersion = "10.1.12"
val any23Version = "2.3"
val commonsCsvVersion = "1.8"

  val bananaDependency0 = "org.w3" %%  "banana-jena" % "0.8.4-SNAPSHOT" 
  val bananaDependency = bananaDependency0 exclude("org.slf4j", "slf4j-api" )	exclude("org.slf4j", "slf4j-log4j12")	exclude("org.apache.logging.log4j","log4j-slf4j-impl")
  val jenaDependency = "org.apache.jena" % "apache-jena-libs" % jenaVersion exclude("org.slf4j", "slf4j-api")
  val jenaPermissionsDependency = "org.apache.jena" % "jena-permissions" % jenaVersion	
  val jenaTextDependency = "org.apache.jena" % "jena-text" % jenaVersion
  val jenaSpatialDependency = "org.apache.jena" % "jena-geosparql" % jenaVersion


  val xmlDependency = "org.scala-lang.modules" %% "scala-xml" % "1.3.0"

  val junitDependency = "junit" % "junit" % "4.13" % Test
  libraryDependencies += "org.scalactic" %% "scalactic" % "3.1.1"
  val scalatestDependency = "org.scalatest" %% "scalatest" % "3.1.1" % "test"
  logBuffered in Test := false

  val loggingDependencies = Seq(
   "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
 , "ch.qos.logback" % "logback-classic" % "1.2.3"

)

  // val scalazDependency = "org.scalaz" %% "scalaz-core" % "7.2.8"
  val scalazDependency = "org.scalaz" %% "scalaz-core" % "7.2.26"

  // allready in Banana
  // val httpComponents = "org.apache.httpcomponents" % "httpclient" % "4.5.2"

  val any23Dependencies = "org.apache.any23" % "apache-any23-csvutils" % any23Version

  val commonDependencies =
    loggingDependencies ++
    Seq(bananaDependency,
	jenaDependency, jenaPermissionsDependency, jenaTextDependency,
    jenaSpatialDependency,
    xmlDependency,
    junitDependency, scalatestDependency,

    "com.typesafe.play" %% "play-json" %  "2.8.1" , // 2.6.14",
    // "com.typesafe.play" %% "play-iteratees" % "2.6.1",

    "org.apache.lucene" % "lucene-suggest" % "7.7.2" , // 7.4.0" , // "6.4.1" , // "7.5.0", // 6.6.5",
    "org.apache.lucene" % "lucene-demo"    % "7.7.2" , // 7.4.0" , // "6.4.1" , // "7.5.0", // 6.6.5",
/*
    "org.apache.lucene" % "lucene-suggest" % "4.9.1" ,
    "org.apache.lucene" % "lucene-demo" % "4.9.1" ,
*/

    // "net.rootdev" % "java-rdfa" % "0.4.3-SNAPSHOT"
    "net.rootdev" % "java-rdfa" % "0.4.2" ,

    "org.scala-lang.modules" %% "scala-async" % "0.9.7" ,

    "org.apache.commons" % "commons-csv" % "1.8" ,
    any23Dependencies
    // for Java 9 (works also with Java 8)
    , "javax.xml.bind" % "jaxb-api" % "2.3.1"
    , "org.jsoup" % "jsoup" % "1.13.1"
  )

}

