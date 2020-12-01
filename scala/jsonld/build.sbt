import Common._


javacOptions in ThisBuild := Seq("-target","11")

libraryDependencies ++= Seq(
    // "com.apicatalog" % "titanium-json-ld" % "0.9-SNAPSHOT" ,
    // "org.glassfish" % "jakarta.json" % "2.0.0" ,
    jenaDependency
) ++
jsonldDependencies ++
jsonDependencies

publishArtifact in (Compile, packageDoc) := false
sources in (Compile,doc) := Seq.empty
