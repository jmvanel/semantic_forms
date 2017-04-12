import sbt.Keys._
import sbt._
import Common._

name := "connectors"

libraryDependencies ++= Seq(
    "org.apache.any23" % "apache-any23-csvutils" %  "1.1" ,
    "org.apache.commons" % "commons-csv" % "1.4" ,
    bananaDependency
)
