import Common._

libraryDependencies ++= Seq(
    bananaDependency
)

publishArtifact in (Compile, packageDoc) := false
sources in (Compile,doc) := Seq.empty

