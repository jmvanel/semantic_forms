import Common._

libraryDependencies ++= Seq(
    bananaDependency,
    jenaDependency
)
libraryDependencies ++= loggingDependencies

publishArtifact in (Compile, packageDoc) := false
sources in (Compile,doc) := Seq.empty

