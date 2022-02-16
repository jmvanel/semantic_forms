import sbt._
import Keys._

object Common {
    // resolvers += Resolver.mavenLocal
    val localResolver = Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)
    val bananaResolver = "bblfish-snapshots" at "http://bblfish.net/work/repo/releases"

// val jenaVersion =  "4.3.2"
val jenaVersion =  "4.4.0"

val akkaVersion = "2.6.17" // 8"
val akkaHttpVersion = "10.2.7" // "10.2.4"
val any23Version = "2.5" // 3"
val commonsCsvVersion = "1.8"
val luceneVersion = "8.10.1" // must be Jena's Lucene version
val bananaVersion = "0.8.4-SNAPSHOT"
val bananaOrganisation = "net.bblfish.rdf"

  // val bananaDependency0 = "org.w3" %%  "banana-jena" % "0.8.4-SNAPSHOT"  exclude("com.fasterxml.jackson.core", "jackson-databind")
  val bananaDependency0 = bananaOrganisation %%  "banana-jena" % bananaVersion exclude("com.fasterxml.jackson.core", "jackson-databind")
  val bananaDependency = bananaDependency0 exclude("org.slf4j", "slf4j-api" )	exclude("org.slf4j", "slf4j-log4j12")	exclude("org.apache.logging.log4j","log4j-slf4j-impl")

  val jenaDependency = "org.apache.jena" % "apache-jena-libs" % jenaVersion exclude("org.slf4j", "slf4j-api") exclude("com.fasterxml.jackson.core", "jackson-databind")
  val jenaPermissionsDependency = "org.apache.jena" % "jena-permissions" % jenaVersion  exclude("com.fasterxml.jackson.core", "jackson-databind")
  val jenaTextDependency = "org.apache.jena" % "jena-text" % jenaVersion  exclude("com.fasterxml.jackson.core", "jackson-databind")
  val jenaSpatialDependency = "org.apache.jena" % "jena-geosparql" % jenaVersion  exclude("com.fasterxml.jackson.core", "jackson-databind")

  val xmlDependency = "org.scala-lang.modules" %% "scala-xml" % "1.3.0" // com.typesafe.play:twirl-api_2.13:1.5.1 (depends on 1.2.0)
                                                             // "2.0.1"

  val junitDependency = "junit" % "junit" % "4.13.2" % Test
  libraryDependencies += "org.scalactic" %% "scalactic" % "3.1.1"
  val scalatestDependency = "org.scalatest" %% "scalatest" % "3.2.10" % "test"
    // 3.1.4"
    // 3.1.1"
  Test / logBuffered := false

  val loggingDependencies = Seq(
   "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4"
 , "ch.qos.logback" % "logback-classic" % "1.2.9"
 , "org.slf4j" % "slf4j-api" % "1.7.32" // :if expand("%") == ""|browse confirm w|else|confirm w|endif
 // 0"
)

  // val scalazDependency = "org.scalaz" %% "scalaz-core" % "7.2.8"
  // val scalazDependency = "org.scalaz" %% "scalaz-core" % "7.2.26"
  val scalazDependency = "org.scalaz" %% "scalaz-core" % "7.3.5"

  // allready in Banana
  // val httpComponents = "org.apache.httpcomponents" % "httpclient" % "4.5.13"

  val any23Dependencies = "org.apache.any23" % "apache-any23-csvutils" % any23Version

  val jsonDependencies = Seq(
    // remove play-json ?
    "com.typesafe.play" %% "play-json" % "2.9.2" , // 1" , // "2.8.1"
    //"javax.json" % "javax.json-api" % "1.1.4" ,
    //"jakarta.json" % "jakarta.json-api" % "2.0.0" ,
    "org.glassfish" % "jakarta.json" % "2.0.1" // 0"
  )

  val jsonldDependencies = Seq( "com.apicatalog" % "titanium-json-ld" % "1.1.0"
    // 1.0.3"
    // 1.0.0"
    // "1.0.2-SNAPSHOT"
  )

  val commonDependencies =
    loggingDependencies ++
    jsonDependencies ++
    Seq(bananaDependency,
	jenaDependency, 
        // jenaPermissionsDependency,
        jenaTextDependency,
    jenaSpatialDependency,
    xmlDependency,
    junitDependency, scalatestDependency,

    "org.apache.lucene" % "lucene-suggest" % luceneVersion , // "9.0.0" , // 8.11.0" ,
    "org.apache.lucene" % "lucene-demo"    % luceneVersion , // "9.0.0" , // 8.11.0" ,

    // "net.rootdev" % "java-rdfa" % "0.4.3-SNAPSHOT"
    "net.rootdev" % "java-rdfa" % "0.4.2" ,

    "org.scala-lang.modules" %% "scala-async" % "1.0.1" , // 0.9.7" ,

    "org.apache.commons" % "commons-csv" % "1.9.0" , // 1.8" ,
    any23Dependencies
    // for Java 9 (works also with Java 8)
    , "javax.xml.bind" % "jaxb-api" % "2.3.1"
    , "org.jsoup" % "jsoup" % "1.14.3" // 1.13.1"

    , "commons-io" % "commons-io" % "2.11.0" // 2.8.0"
  )

}

