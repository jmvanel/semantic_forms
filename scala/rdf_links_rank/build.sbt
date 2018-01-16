import Common._

libraryDependencies ++= Seq(
    bananaDependency,
    jenaDependency
)

publishArtifact in (Compile, packageDoc) := false
sources in (Compile,doc) := Seq.empty

