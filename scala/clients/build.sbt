// import sbt.Keys._
// import sbt._
import Common._

name := "clients"

libraryDependencies ++= Seq(
  bananaDependency0,
  jenaDependency,
"com.typesafe.akka" %% "akka-http"   % akkaHttpVersion ,
"com.typesafe.akka" %% "akka-stream" % akkaVersion // or whatever the latest version is
) ++ loggingDependencies
// libraryDependencies += httpComponents // allready in Banana

publishArtifact in (Compile, packageDoc) := false
sources in (Compile,doc) := Seq.empty

