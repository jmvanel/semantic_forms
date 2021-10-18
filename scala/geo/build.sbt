import Common._

ThisBuild / scalaVersion := // "2.12.13"
        "2.13.6"

// ThisBuild / versionScheme := Some("early-semver")
// ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-async" % "early-semver"

libraryDependencies ++= commonDependencies
libraryDependencies ++= jsonldDependencies

// libraryDependencies += "org.scala-lang.modules" %% "scala-async" % "1.0.1" // 0.10.0"
// libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided

// Seq(  "com.apicatalog" % "titanium-json-ld" % "0.9-SNAPSHOT" ,
// "jakarta.json" % "jakarta.json-api" % "2.0.0" ,
// "org.glassfish" % "jakarta.json" % "2.0.0"
// )
// "com.apicatalog" % "titanium-json-ld" % "0.8.6" ,
// libraryDependencies ++= Seq( "com.lihaoyi" %% "upickle" % "0.7.1" )
