import sbt.Keys._
import sbt._
import Common._

name := "web_tests"

lazy val web_tests = (project in file("."))
	.enablePlugins(GatlingPlugin)

// fork a new JVM for 'test:run', but not 'run'
// fork in Test := true
// fork := true
// add a JVM option to use when forking a JVM for 'run'
// javaOptions += "-Xmx50M"

// Gatling is an open-source load testing framework based on Scala, Akka and Netty
libraryDependencies += "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.1.7" % Test
libraryDependencies += "io.gatling" % "gatling-test-framework" % "2.1.7" % Test
// libraryDependencies += "com.github.agourlay" %% "cornichon" % "0.12.1" % Test
libraryDependencies += "com.github.agourlay" %% "cornichon" % "0.12.2-SNAPSHOT" % Test

// resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
resolvers += Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)

// temporary for cornichon SNAPSHOTs
resolvers += "cornichon-sonatype" at "https://oss.sonatype.org/content/repositories/snapshots"

