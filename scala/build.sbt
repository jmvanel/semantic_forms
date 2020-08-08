import Common._

maintainer := "jeanmarc.vanel@gmail.com"

// Discussion on get rid of twirl in playframework
// https://github.com/playframework/playframework/issues/5823

// offline := true
lazy val root = Project("semantic_forms-root", file("."))
    .aggregate(forms_play, forms, core, utils, sparql_cache,
               abstract_syntax, html, clients, rdf_links_rank)

organization in ThisBuild := "deductions"
version in ThisBuild := "2.X-SNAPSHOT"

scalaVersion in ThisBuild := "2.12.12"
// "2.13.3" //

// javacOptions in ThisBuild := Seq("-source","1.8", "-target","1.8")
scalacOptions ++= Seq("-unchecked",
	// "-deprecation",
	"-feature", "-explaintypes", "-language:_", "-Xlint:_")

publishArtifact in (Compile, packageDoc) := false
sources in (Compile,doc) := Seq.empty

lazy val forms_play = (project in file("forms_play"))
	.dependsOn(forms)
	.dependsOn(contacts_management)
//        .dependsOn(mobion)
	.dependsOn(clients)
.enablePlugins(PlayScala) .disablePlugins(PlayLogback)
.settings(
   scalaJSProjects := Seq(forms_js),
   pipelineStages in Assets := Seq(scalaJSPipeline),
   // pipelineStages := Seq(digest, gzip),
// triggers scalaJSPipeline when using compile or continuous compilation
   compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
   libraryDependencies ++= Seq(
     ( "com.vmunier" %% "scalajs-scripts" % "1.1.2" ) . exclude("com.typesafe.play", "twirl-api_2.11")
   )
   )

lazy val core = project
lazy val utils = project .dependsOn(core)
lazy val connectors = project .dependsOn(utils)
lazy val sparql_cache = project .dependsOn(utils)
	.dependsOn(connectors)
lazy val abstract_syntax = project .dependsOn(core)   .dependsOn(sparql_cache)
lazy val html = project .dependsOn(utils)
lazy val rdf_links_rank = project .dependsOn(utils)
lazy val clients = project
lazy val forms = project
	.dependsOn(html)
	.dependsOn(abstract_syntax)
	// .dependsOn(connectors)
	.dependsOn(rdf_links_rank)

// lazy val web_tests = project
lazy val forms_js = project .settings(
  scalaJSUseMainModuleInitializer := true,
  // emitSourceMaps in fastOptJS := true
  isDevMode in scalaJSPipeline := true
).enablePlugins(ScalaJSPlugin, ScalaJSWeb)

lazy val generic_app = project
lazy val projects_catalog = project
lazy val contacts_management = project .dependsOn(forms)

// lazy val mobion = project .dependsOn(forms)
// lazy val jsonld = project

// lazy val forms_services = project
// lazy val social_web = project
// lazy val sparql_client = project

// Added for locally compiled Java-RDFa:
// resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
resolvers in ThisBuild += Resolver.mavenLocal
resolvers += Resolver.url("typesafe", url("https://repo.typesafe.com/typesafe/ivy-releases/"))(Resolver.ivyStylePatterns)

// PENDING: really necessary?
// resolvers in ThisBuild += Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)
// resolvers in ThisBuild += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

// loads the server project at sbt startup
onLoad in Global := (onLoad in Global).value andThen {s: State => "project forms_play" :: s}

// Scala code checkers

// wartremoverErrors ++= Warts.unsafe
// wartremoverErrors ++= Warts.allBut(Wart.DefaultArguments, Wart.Var)
// libraryDependencies += "com.lightbend" %% "abide-core" % "0.1-SNAPSHOT" % "abide"

