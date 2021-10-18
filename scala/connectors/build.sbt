// import sbt.Keys._
// import sbt._
import Common._

name := "connectors"

libraryDependencies ++= Seq(
    any23Dependencies ,
    "org.apache.commons" % "commons-csv" % commonsCsvVersion ,
    bananaDependency,
    jenaDependency
)

