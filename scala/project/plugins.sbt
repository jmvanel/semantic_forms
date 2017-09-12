// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository 
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects

// addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.15")
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.16")

// See https://www.playframework.com/documentation/2.5.x/StreamsMigration25
// http://pastebin.com/MkkWLGL1

// Let sbt eclipse plugin be available to eclipse users
addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "5.1.0")

// addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.18")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.20")
addSbtPlugin("io.gatling" % "gatling-sbt" % "2.1.7")


// Comment to get more information during initialization
logLevel := Level.Warn



// play-with-scalajs-example

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.3")
addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % "1.0.5")
addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.1")
// addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "4.0.0")

