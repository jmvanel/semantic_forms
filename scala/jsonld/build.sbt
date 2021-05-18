import Common._

ThisBuild / javacOptions := Seq("-target","11")

libraryDependencies ++= Seq(
    // "com.apicatalog" % "titanium-json-ld" % "0.9-SNAPSHOT" ,
    // "org.glassfish" % "jakarta.json" % "2.0.0" ,
    jenaDependency
) ++
jsonldDependencies ++
jsonDependencies

// Compile / publishArtifact := false
// packageDoc / publishArtifact := false
// Compile / sources := Seq.empty
// doc / sources := Seq.empty

// publishArtifact in (Compile, packageDoc) := false
// sources in (Compile,doc) := Seq.empty
