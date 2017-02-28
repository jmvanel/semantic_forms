organization := "deductions"
name := "semantic_forms_play"
version := "1.0-SNAPSHOT"

lazy val semantic_forms =  RootProject(file("../forms"))
lazy val semantic_forms_play = (project in file("."))
        .dependsOn(semantic_forms)
	.enablePlugins(PlayScala)
  .disablePlugins(PlayLogback)

scalaVersion := "2.11.8"
// scalaVersion :=  "2.12.1"
javacOptions ++= Seq("-source","1.7", "-target","1.7")

        publishArtifact in (Compile, packageDoc) := false
        publishArtifact in packageDoc := false
        sources in (Compile,doc) := Seq.empty

fork in run := true
// CAUTION here: this is for run and runMain; what is given with sbt -J-Xmx12G is *not* in effect for run! 
javaOptions in run ++= Seq( "-Xms256M", "-Xmx8G", "-XX:MaxPermSize=1024M", "-XX:+UseConcMarkSweepGC")

connectInput in run := true

routesGenerator := StaticRoutesGenerator

// libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.6" % Test
libraryDependencies += "org.scalatestplus" %% "play" % "1.4.0" % Test
// libraryDependencies += specs2 % Test

sources in (Compile, doc) := Seq.empty
publishArtifact in (Compile, packageDoc) := false

// fork a new JVM for 'test:run' and 'run'
fork := true
// add a JVM option to use when forking a JVM for 'run'
javaOptions += "-Xmx50M"

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
resolvers += Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)
// cf http://stackoverflow.com/questions/16400877/local-dependencies-resolved-by-sbt-but-not-by-play-framework
// resolvers += Resolver.mavenLocal

// see http://www.scalatest.org/user_guide/using_scalatest_with_sbt
// show full stack trace in SBT
// testOptions in Test += Tests.Argument("-oF")

