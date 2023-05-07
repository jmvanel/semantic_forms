// Comment to get more information during initialization
logLevel := Level.Warn

// Use the Play sbt plugin for Play projects
// addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.9")
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.19") // 8")

// FIX build error involving org.scala-lang.modules:scala-xml version 2.1.0
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
addSbtPlugin("com.typesafe.play" % "sbt-twirl"           % "1.5.2")
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.16")
// see https://github.com/playframework/playframework/issues/11522

// addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.0.0")
// addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % "1.0.11")

addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % "1.1.0")
//addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.7.0")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.9.0")

