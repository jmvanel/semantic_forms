import Common._

// Discussion on get rid of twirl in playframework
// https://github.com/playframework/playframework/issues/5823

// offline := true

name := "semantic_forms-root"

organization in ThisBuild := "deductions"
version in ThisBuild := "2.1-SNAPSHOT"

scalaVersion in ThisBuild := "2.11.11" // "2.12.3"
javacOptions in ThisBuild := Seq("-source","1.8", "-target","1.8")
scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-explaintypes", "-language:_", "-Xlint:_")

lazy val forms_play = (project in file("forms_play"))
	.dependsOn(forms)
	.dependsOn(contacts_management)
	.dependsOn(mobion)
.enablePlugins(PlayScala) .disablePlugins(PlayLogback)
.settings(
   scalaJSProjects := Seq(forms_js),
   pipelineStages in Assets := Seq(scalaJSPipeline),
   pipelineStages := Seq(digest, gzip),
// triggers scalaJSPipeline when using compile or continuous compilation
   compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
   libraryDependencies ++= Seq(
     ( "com.vmunier" %% "scalajs-scripts" % "1.1.1" ) . exclude("com.typesafe.play", "twirl-api_2.11")
   )
   )
/*
[warn] Found version conflict(s) in library dependencies; some are suspected to be binary incompatible:
[warn] 
[warn] 	* com.typesafe.play:twirl-api_2.11:1.3.3 is selected over 1.1.1
[warn] 	    +- com.vmunier:scalajs-scripts_2.11:1.1.1             (depends on 1.3.3)
[warn] 	    +- deductions:semantic_forms_play_2.11:2.1-SNAPSHOT   (depends on 1.1.1)
[warn] 	    +- com.typesafe.play:play_2.11:2.5.15                 (depends on 1.1.1)
*/

lazy val core = project
lazy val utils = project .dependsOn(core)
lazy val connectors = project .dependsOn(utils)
lazy val sparql_cache = project .dependsOn(utils)
lazy val abstract_syntax = project .dependsOn(core)   .dependsOn(sparql_cache)
lazy val html = project .dependsOn(utils)
lazy val rdf_links_rank = project .dependsOn(utils)
lazy val forms = project
	.dependsOn(html)
	.dependsOn(abstract_syntax)
	.dependsOn(connectors)
	.dependsOn(rdf_links_rank)

lazy val web_tests = project
lazy val forms_js = project .settings(
  scalaJSUseMainModuleInitializer := true,
  emitSourceMaps in fastOptJS := true
).enablePlugins(ScalaJSPlugin, ScalaJSWeb)

lazy val generic_app = project
lazy val projects_catalog = project
lazy val contacts_management = project .dependsOn(forms)
lazy val mobion = project .dependsOn(forms)

// lazy val forms_services = project
// lazy val social_web = project
// lazy val sparql_client = project

// PENDING: really necessary?
// resolvers in ThisBuild += Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)
// resolvers in ThisBuild += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

// loads the server project at sbt startup
onLoad in Global := (Command.process("project forms_play", _: State)) compose (onLoad in Global).value

// Scala code checkers

// wartremoverErrors ++= Warts.unsafe
// wartremoverErrors ++= Warts.allBut(Wart.DefaultArguments, Wart.Var)
// libraryDependencies += "com.lightbend" %% "abide-core" % "0.1-SNAPSHOT" % "abide"

