addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.3.0")

// Let sbt eclipse plugin be available to eclipse users
// addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "4.0.0")
addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "5.0.1")

addSbtPlugin("org.wartremover" % "sbt-wartremover" % "1.3.0")
// must be commented out, bacause it is not released:
// addSbtPlugin("com.lightbend" % "sbt-abide" % "0.1-SNAPSHOT")
