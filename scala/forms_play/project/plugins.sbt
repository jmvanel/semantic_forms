// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository 
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects
// addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.10") // 9")
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.4.6")
// addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.0")
// See https://www.playframework.com/documentation/2.5.x/StreamsMigration25
// http://pastebin.com/MkkWLGL1

// Let sbt eclipse plugin be available to eclipse users
addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "3.0.0")


