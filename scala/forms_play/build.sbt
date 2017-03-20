import sbt.Keys._
import sbt._
import Common._

name := "semantic_forms_play"

// necessary for running from this directory:
// lazy val forms =  RootProject(file("../forms"))

// for running from parent directory
lazy val forms = (project in file("../forms"))

lazy val forms_play = (project in file("."))
        .dependsOn(forms)
	.enablePlugins(PlayScala)
  .disablePlugins(PlayLogback)

        publishArtifact in (Compile, packageDoc) := false
        publishArtifact in packageDoc := false
        sources in (Compile,doc) := Seq.empty

fork in run := true
// CAUTION here: this is for run and runMain; what is given with sbt -J-Xmx12G is *not* in effect for run! 
javaOptions in run ++= Seq( "-Xms256M", "-Xmx8G", "-XX:MaxPermSize=1024M", "-XX:+UseConcMarkSweepGC")
baseDirectory in run := file(".") // does not work: the TDB files are in parent dir.

connectInput in run := true

routesGenerator := StaticRoutesGenerator

libraryDependencies += "org.scalatestplus" %% "play" % "1.4.0" % Test

sources in (Compile, doc) := Seq.empty
publishArtifact in (Compile, packageDoc) := false

// fork a new JVM for 'test:run' and 'run'
fork := true
// add a JVM option to use when forking a JVM for 'run'
javaOptions += "-Xmx50M"

// PENDING: really necessary? (also in ../build.sbt)
resolvers += Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)
// cf http://stackoverflow.com/questions/16400877/local-dependencies-resolved-by-sbt-but-not-by-play-framework
// resolvers += Resolver.mavenLocal

// see http://www.scalatest.org/user_guide/using_scalatest_with_sbt
// show full stack trace in SBT
// testOptions in Test += Tests.Argument("-oF")

