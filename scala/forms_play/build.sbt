import sbt.Keys._
import sbt._
import Common._

name := "semantic_forms_play"

publishArtifact in (Compile, packageDoc) := false
publishArtifact in packageDoc := false
sources in (Compile,doc) := Seq.empty

// CAUTION here: this is for run and runMain; what is given with sbt -J-Xmx12G is *not* in effect for run! 
javaOptions in run ++= Seq( "-Xms256M", "-Xmx8G", "-XX:MaxPermSize=1024M", "-XX:+UseConcMarkSweepGC")
baseDirectory in run := file(".") // does not work: the TDB files are in parent dir.

connectInput in run := true

routesGenerator := StaticRoutesGenerator

libraryDependencies += "org.scalatestplus" %% "play" % "1.4.0" % Test

sources in (Compile, doc) := Seq.empty
publishArtifact in (Compile, packageDoc) := false

// fork a new JVM for 'test:run' and 'run'
fork in run := true
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


// From https://github.com/vmunier/play-with-scalajs-example

// lazy val forms_js = (project in file("forms_js"))
  // scalaJSProjects := Seq(forms_js)
  // pipelineStages in Assets := Seq(scalaJSPipeline)
  // pipelineStages := Seq(digest, gzip)
  // triggers scalaJSPipeline when using compile or continuous compilation
  // compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value
  // libraryDependencies ++= Seq(
    "com.vmunier" %% "scalajs-scripts" % "1.1.1"
    // guice, specs2 % Test
 //  )
  // Compile the project before generating Eclipse files, so that generated .scala or .class files for views and routes are present
  // EclipseKeys.preTasks := Seq(compile in Compile)
