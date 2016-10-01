package deductions.runtime.sparql_cache.algos

import java.io.PrintStream
import java.io.FileReader
import java.io.FileWriter
import java.io.FileOutputStream
import deductions.runtime.services.DefaultConfiguration
import deductions.runtime.jena.ImplementationSettings

/**
 * ReplaceSubclassWithProperty : App Example with hard-coded list of super-classes
 * to disconnect from their sub-classes
 */
object ReplaceSubclassWithPropertyAppExample extends App
    with ImplementationSettings.RDFModule
    with DefaultConfiguration
    with DuplicatesDetectionOWL[ImplementationSettings.Rdf]
    with ReplaceSubclassWithProperty[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with WrongSubclassesSelection[ImplementationSettings.Rdf] {

  //  val printStream: PrintStream = ???
  val owlFile = args(0)
  val graph = turtleReader.read(new FileReader(owlFile), "").get

  import ops._

  val superClasses0 = for (
    classeTriple <- ops.find(graph, ANY, rdf.typ, owl.Class);
    classe = classeTriple.subject;
    label = rdfsLabel(classe, graph) if (
      // hard-coded list of super-classes TODO
      label.startsWith("Onglet") ||
      label.startsWith("Emplacement"))
  ) yield { classe }

  val superClasses = superClasses0.map {
    n => makeURIFromNode(n)
  }.toList

  val classPairs = getWrongSubclassePairs()
  println(s"class Pairs: ${classPairs.mkString(", ")}")
  replaceSubclasses(graph, classPairs)

  val outputFile = owlFile + ".ReplaceSubclass.ttl"
  turtleWriter.write(graph, new FileOutputStream(outputFile), "")
  println(s"output File $outputFile: graph size ${graph.size}")

  override def getWrongSubclassePairs(): List[(Rdf#URI, Rdf#URI)] = {
    makeWrongSubclassePairsFromSuperClasses(superClasses, graph)
  }
}