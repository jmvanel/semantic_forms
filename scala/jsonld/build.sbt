import Common._


javacOptions in ThisBuild := Seq("-target","11")

libraryDependencies ++= Seq(
    // "com.apicatalog" % "titanium-json-ld" % "0.8.5-SNAPSHOT" ,
    "com.apicatalog" % "titanium-json-ld" % "0.8.5" ,
    "org.glassfish" % "jakarta.json" % "1.1.6" ,
    jenaDependency
)

publishArtifact in (Compile, packageDoc) := false
sources in (Compile,doc) := Seq.empty
