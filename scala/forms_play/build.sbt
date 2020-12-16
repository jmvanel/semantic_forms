import sbt.Keys._
import sbt._
import Common._

// offline := true

name := "semantic_forms_play"
maintainer := "jeanmarc.vanel@gmail.com"
scalacOptions ++= Seq(
	// "-unchecked",
        // "-deprecation",
        // "-feature", "-explaintypes", "-language:_", 
	// "-Xlint:unused"
// "-J-g:none"
)

publishArtifact in (Compile, packageDoc) := false
sources in (Compile,doc) := Seq.empty

// CAUTION here: this is for run and runMain; what is given with sbt -J-Xmx12G is *not* in effect for run! 
// javaOptions in run ++= Seq( "-Xms256M", "-Xmx8G", "-XX:MaxPermSize=1024M", "-XX:+UseConcMarkSweepGC")
baseDirectory in run := file(".") // does not work: the TDB files are in parent dir.

connectInput in run := true

// routesGenerator := StaticRoutesGenerator
// routesGenerator := InjectedRoutesGenerator
// 
libraryDependencies += guice

// libraryDependencies += filters
// libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % "test"
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % "test"

libraryDependencies += "com.typesafe.play" %% "play-test" % play.core.PlayVersion.current % "test"
//libraryDependencies += "com.typesafe.play" %% "play-test" % "2.8.5" % "test"

// For debug breakpoints ???
fork in run := false

// fork a new JVM for 'test:run' and 'run'
// fork in run := true
// fork := true
// add a JVM option to use when forking a JVM for 'run'
// javaOptions += "-Xmx50M"

// PENDING: really necessary? (also in ../build.sbt)
resolvers += Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)
// cf http://stackoverflow.com/questions/16400877/local-dependencies-resolved-by-sbt-but-not-by-play-framework
// resolvers += Resolver.mavenLocal

// see http://www.scalatest.org/user_guide/using_scalatest_with_sbt
// show full stack trace in SBT
// testOptions in Test += Tests.Argument("-oF")

// PlayKeys.devSettings += "play.server.http.port" -> "9111"

PlayKeys.devSettings := Seq("play.akka.dev-mode.akka.http.parsing.max-uri-length" -> "20480")

