package deductions.runtime.sparql_cache.algos

import org.w3.banana.jena.Jena
import org.w3.banana.jena.JenaModule
import java.io.PrintStream
import java.io.FileReader
import java.io.FileWriter
import java.io.FileOutputStream

/**
 * ReplaceSubclassWithProperty : App Example with hard-coded list of super-classes
 * to disconnect from their sub-classes
 */
object ReplaceSubclassWithPropertyAppExample extends App
    with JenaModule
    with DuplicatesDetectionOWL[Jena]
    with ReplaceSubclassWithProperty[Jena, AnyRef]
    with WrongSubclassesSelection[Jena] {
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
  turtleWriter.write(graph, new FileOutputStream(owlFile + ".ReplaceSubclass.ttl"), "")

  override def getWrongSubclassePairs(): List[(Rdf#URI, Rdf#URI)] = {
    makeWrongSubclassePairsFromSuperClasses(superClasses, graph)
  }

}