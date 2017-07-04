package deductions.runtime.sparql_cache.algos

import java.io.{FileInputStream, FileOutputStream}

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.utils.DefaultConfiguration
import org.w3.banana.OWLPrefix

/**
 * ReplaceSubclassWithProperty : App Example with hard-coded criterion on super-classes
 * to be disconnected from their sub-classes
 */
object ReplaceSubclassWithPropertyAppExample extends {
  override val config = new DefaultConfiguration {
    override val useTextQuery = false
  }
} with App
    with ImplementationSettings.RDFCache
    with DefaultConfiguration
    with DuplicatesDetectionOWL[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with ReplaceSubclassWithProperty[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    with WrongSubclassesSelection[ImplementationSettings.Rdf] {

  val owlFile = args(0)
  val graph = turtleReader.read(new FileInputStream(owlFile), "").get

  import ops._
  private lazy val owl = OWLPrefix[Rdf]

  /** hard-coded criterion on super-classes
 * to be disconnected from their sub-classes
 * @return list of super-classes */
  def getSuperClassesToProcess() = {
    val superClasses0 = for (
      classeTriple <- ops.find(graph, ANY, rdf.typ, owl.Class);
      classe = classeTriple.subject;
      superClassesTriples <- ops.find(graph, classe, rdfs.subClassOf, ANY);
      superClass = superClassesTriples.objectt;
      label = rdfsLabel(classe, graph) if (
        label.startsWith("Onglet") ||
        label.startsWith("Emplacement")) ||
        fromUri(uriNodeToURI(superClass)).endsWith("_spécifiques")
    ) yield { classe }

    superClasses0.map {
      n => makeURIFromNode(n)
    }.toList
  }

  val superClasses = getSuperClassesToProcess()
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