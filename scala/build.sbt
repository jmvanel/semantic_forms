import Common._

name := "semantic_forms-root"

organization in ThisBuild := "deductions"
version in ThisBuild := "1.0-SNAPSHOT" // "1.10-SNAPSHOT"

scalaVersion in ThisBuild := "2.11.8" // scalaVersion :=  "2.12.1"
javacOptions in ThisBuild := Seq("-source","1.8", "-target","1.8")
scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-explaintypes", "-language:_", "-Xlint:_")

lazy val forms_play = (project in file("forms_play"))
	.dependsOn(forms)
	.dependsOn(forms_js)
	.enablePlugins(PlayScala) .disablePlugins(PlayLogback)

lazy val core = project
lazy val generic_app = project
lazy val projects_catalog = project
lazy val contacts_management = project .dependsOn(utils)
lazy val connectors = project .dependsOn(utils)
lazy val utils = project
lazy val forms = project.in(file("forms")) .dependsOn(connectors) .aggregate(connectors)

// lazy val web_tests = project
lazy val forms_js = project
// lazy val forms_services = project
// lazy val social_web = project
// lazy val sparql_client = project

// PENDING: really necessary?
resolvers in ThisBuild += Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)
resolvers in ThisBuild += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

// Scala code checkers

// wartremoverErrors ++= Warts.unsafe
// wartremoverErrors ++= Warts.allBut(Wart.DefaultArguments, Wart.Var)
// libraryDependencies += "com.lightbend" %% "abide-core" % "0.1-SNAPSHOT" % "abide"

