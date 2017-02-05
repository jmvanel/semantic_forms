// import Common._

organization := "deductions"
name := "semantic_forms"
version := "1.0-SNAPSHOT"

//lazy val forms_js =  RootProject(file("../forms_js"))
//lazy val semantic_forms = (project in file("."))
//        .dependsOn(forms_js)

scalaVersion :=  "2.11.8"
// scalaVersion :=  "2.12.0"
val jenaVersion =  "3.1.0"

javacOptions ++= Seq("-source","1.7", "-target","1.7")
scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

libraryDependencies += "org.w3" %%  "banana-jena" % "0.9.0-SNAPSHOT"

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
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.3.10"
// libraryDependencies += "com.netaporter" %% "scala-i18n" % "0.1"
libraryDependencies += "org.apache.commons" % "commons-csv" % "1.4"
libraryDependencies += "org.apache.any23" % "apache-any23-csvutils" %  "1.1" // "1.0" // 

libraryDependencies ++= Seq(
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.8",
  "org.apache.logging.log4j" % "log4j-api" % "2.8",
  "org.apache.logging.log4j" % "log4j-core" % "2.8"
)

// for debugging the SBT test suite:
// required for the javaOptions to be passed in (??)
// fork := true

// javaOptions in test ++= Seq( "-Xdebug" , "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=9999" )

parallelExecution in Test := false

libraryDependencies += "junit" % "junit" % "4.12" % Test
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % Test

// resolvers += "spray repo" at "http://repo.spray.io"
// resolvers += Resolver.url("inthenow-releases", url("http://dl.bintray.com/inthenow/releases"))(Resolver.ivyStylePatterns)

// outdated:  resolvers += "betehess" at  "http://dl.bintray.com/banana-rdf/banana-rdf"

resolvers += "apache-repo-releases" at "https://repository.apache.org/content/repositories/releases/"

// resolvers += Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)
// resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"
// resolvers += Resolver.mavenLocal
// resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

// for i18n? resolvers += "bintray" at "http://jcenter.bintray.com"
scalariformSettings
