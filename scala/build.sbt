import Common._

maintainer := "jeanmarc.vanel@gmail.com"

// Discussion on get rid of twirl in playframework
// https://github.com/playframework/playframework/issues/5823

// offline := true
lazy val root = Project("semantic_forms-root", file("."))
    .aggregate(forms_play, forms, core, utils, sparql_cache,
               abstract_syntax, html, clients, rdf_links_rank)

ThisBuild / organization := "deductions"
ThisBuild / version := "2.X-SNAPSHOT"

ThisBuild / scalaVersion :=
	// "2.13.7"
	// "2.13.8"
	"2.13.10"

ThisBuild / scalacOptions ++= Seq(
	// "-deprecation"
	// ,
	// "-feature"
)

// ThisBuild / versionScheme := Some("early-semver")
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-async" % "early-semver"

// 
ThisBuild / javacOptions := Seq("-source","1.8", "-target","1.11")
// scalacOptions ++= Seq("-unchecked", // "-deprecation", "-feature", "-explaintypes", "-language:_", "-Xlint:unused")

Compile / publishArtifact := false
packageDoc / publishArtifact := false
Compile / sources := Seq.empty
doc / sources := Seq.empty

lazy val forms_play = (project in file("forms_play"))
	.dependsOn(forms)
	.dependsOn(contacts_management)
//        .dependsOn(mobion)
	.dependsOn(clients)

.enablePlugins(PlayScala) .disablePlugins(PlayLogback)
.settings(
   scalaJSProjects := Seq(forms_js),
   Assets / pipelineStages := Seq(scalaJSPipeline),
   // pipelineStages := Seq(digest, gzip),
// triggers scalaJSPipeline when using compile or continuous compilation
   Compile / compile := ((Compile / compile) dependsOn scalaJSPipeline).value,
   libraryDependencies ++= Seq(
     ( "com.vmunier" %% "scalajs-scripts" % "1.2.0" )
   )
)

lazy val core = project
lazy val utils = project .dependsOn(core)
lazy val connectors = project .dependsOn(utils)
lazy val sparql_cache = project .dependsOn(utils)
	.dependsOn(connectors)
	.dependsOn(geo)
lazy val abstract_syntax = project .dependsOn(core)   .dependsOn(sparql_cache)
lazy val html = project .dependsOn(utils)
lazy val rdf_links_rank = project .dependsOn(utils)
lazy val clients = project
lazy val forms = project
	.dependsOn(html)
	.dependsOn(abstract_syntax)
	// .dependsOn(connectors)
	.dependsOn(rdf_links_rank)
	.dependsOn(jsonld)

// lazy val web_tests = project
lazy val forms_js = project .settings(
  // scalaJSUseMainModuleInitializer := true,
  // emitSourceMaps in fastOptJS := true
  // isDevMode in scalaJSPipeline := true
).enablePlugins(ScalaJSPlugin, ScalaJSWeb)

lazy val generic_app = project
lazy val projects_catalog = project
lazy val contacts_management = project .dependsOn(forms)

// lazy val mobion = project .dependsOn(forms)
lazy val jsonld = project
lazy val geo = project . dependsOn( jsonld )

// lazy val forms_services = project
// lazy val social_web = project
// lazy val sparql_client = project

// Added for locally compiled Java with Maven (e.g. Jena)
// resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
Global / resolvers += Resolver.mavenLocal

// ???
// resolvers in ThisBuild += Resolver.mavenLocal

// resolvers += Resolver.url("typesafe", url("https://repo.typesafe.com/typesafe/ivy-releases/"))(Resolver.ivyStylePatterns)

// PENDING: really necessary?
// resolvers in ThisBuild += Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)
// resolvers in ThisBuild += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

// loads the server project at sbt startup
Global / onLoad := (onLoad in Global).value andThen {s: State => "project forms_play" :: s}

// Scala code checkers

// wartremoverErrors ++= Warts.unsafe
// wartremoverErrors ++= Warts.allBut(Wart.DefaultArguments, Wart.Var)
// libraryDependencies += "com.lightbend" %% "abide-core" % "0.1-SNAPSHOT" % "abide"

