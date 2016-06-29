package deductions.runtime.sparql_cache.algos

import org.w3.banana.RDF
import org.w3.banana.RDFOps
import org.w3.banana.RDFPrefix
import org.w3.banana.RDFSPrefix
import org.w3.banana.OWLPrefix
import org.w3.banana.jena.Jena
import org.w3.banana.jena.JenaModule
import java.io.FileReader
import org.w3.banana.PointedGraph
import deductions.runtime.utils.CSVImporter

object ReplaceSubclassWithPropertyApp extends App with JenaModule with ReplaceSubclassWithProperty[Jena, Any] 
{
  val owlFile = args(0)
  val graph = turtleReader.read(new FileReader(owlFile), "").get
  val mgraph = ops.makeMGraph(graph)
  val tableClassesFile = args(1)
  val pairs: List[(Rdf#URI, Rdf#URI)] = readCSVFile(tableClassesFile)
  for ((subClass: Rdf#URI, superClass: Rdf#URI) <- pairs) {
    replaceSubclass(mgraph, subClass, superClass)
  }
}

trait ReplaceSubclassWithProperty[Rdf <: RDF, DATASET] // TODO extends CSVImporter[Rdf, DATASET]
{
  val detailedLog = false

  implicit val ops: RDFOps[Rdf]
  import ops._
  val rdf = RDFPrefix[Rdf]
  val rdfs = RDFSPrefix[Rdf]
  val owl = OWLPrefix[Rdf]

  /**
   * input such that
   *
   *  subClass rdfs:subClassOf superClass
   */
  def replaceSubclass(graph: Rdf#MGraph, subClass: Rdf#URI, superClass: Rdf#URI) = {

    removeSubClassOf
    createProperty

    def removeSubClassOf = {
      println(s"subClass subClass")
      println(s"superClass $superClass")
      removeTriple(graph, Triple(subClass, rdfs.subClassOf, superClass))
    }

    def createProperty = {
      val newProperty = fromUri(superClass) + "#prop"
      val labelPG = PointedGraph(subClass) / rdfs.label
      val label = labelPG.nodes.head
      val newGraph = (URI(newProperty)
        -- rdf.typ ->- owl.ObjectProperty
        -- rdfs.domain ->- subClass
        -- rdfs.range ->- superClass
        -- rdfs.label ->- label).graph
      addTriples(graph, newGraph.triples)
    }
  }

  /** read CSV file with columns owl:Class & rdfs.subClassOf : inheritances to transform */
  def readCSVFile(tableClassesFile: String): List[(Rdf#URI, Rdf#URI)] = {
    ???
  }
}