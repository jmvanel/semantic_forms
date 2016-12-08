package deductions.runtime.sparql_cache.algos

import org.w3.banana.RDF
import org.w3.banana.RDFOps
import org.w3.banana.RDFPrefix
import org.w3.banana.RDFSPrefix
import org.w3.banana.OWLPrefix
import org.w3.banana.jena.Jena
import org.w3.banana.jena.JenaModule
import org.w3.banana.PointedGraph
import deductions.runtime.utils.CSVImporter
import java.io.FileInputStream
import deductions.runtime.services.DefaultConfiguration

object ReplaceSubclassWithPropertyApp extends App
with JenaModule
with DefaultConfiguration
with ReplaceSubclassWithProperty[Jena, Any] 
{
  val owlFile = args(0)
  val graph = turtleReader.read(new FileInputStream(owlFile), "").get
  val mgraph = ops.makeMGraph(graph)
  val tableClassesFile = args(1)
  val pairs: List[(Rdf#URI, Rdf#URI)] = readCSVFile(tableClassesFile)
  for ((subClass: Rdf#URI, superClass: Rdf#URI) <- pairs) {
    replaceSubclass(mgraph, subClass, superClass)
  }
}

trait WrongSubclassesSelection[Rdf <: RDF] {
  implicit val ops: RDFOps[Rdf]
  import ops._
  val rdfs: RDFSPrefix[Rdf]

  def getWrongSubclassePairs(): List[(Rdf#URI, Rdf#URI)]

  /** @return List of pairs (subClass, superClass) */
  def makeWrongSubclassePairsFromSuperClasses(superClasses: List[Rdf#URI],
                                              graph: Rdf#Graph): List[(Rdf#URI, Rdf#URI)] = {
    val list = for (
      superClass <- superClasses;
      subClass <- find(graph, ANY, rdfs.subClassOf, superClass)
    ) yield ( makeURIFromNode(subClass.subject) , superClass)
    list
  }

  def makeURIFromNode(n: Rdf#Node): Rdf#URI =
    foldNode(n)(
        uri => uri,
        bn => URI(""),
        lit => URI(""))
}

trait ReplaceSubclassWithProperty[Rdf <: RDF, DATASET]
extends CSVImporter[Rdf, DATASET] {
  implicit val ops: RDFOps[Rdf]
  import ops._
  private val rdf = RDFPrefix[Rdf]
//  private val rdfs = RDFSPrefix[Rdf]
  private val owl = OWLPrefix[Rdf]

  /** @param paris List of pairs (subClass, superClass) */
  def replaceSubclasses(graph: Rdf#MGraph, pairs: List[(Rdf#URI, Rdf#URI)] ) = {
    for(
        pair <- pairs ;
        subClass = pair._1 ;
        superClass = pair._2
    )
      replaceSubclass(graph, subClass, superClass)
  }

  /**
   * input such that
   *
   *  subClass rdfs:subClassOf superClass
   */
  def replaceSubclass(graph: Rdf#MGraph, subClass: Rdf#URI, superClass: Rdf#URI) = {

    removeSubClassOf
    createProperty

    def removeSubClassOf = {
      println(s"subClass $subClass, superClass $superClass")    
      removeTriple(graph, Triple(subClass, rdfs.subClassOf, superClass))
    }

    def createProperty = {
      val newProperty = fromUri(superClass) + "#prop"
      val labelPG = PointedGraph(superClass, graph.makeIGraph()) / rdfs.label
      val label = labelPG.nodes.head
      val newGraph = (URI(newProperty)
        -- rdf.typ ->- owl.ObjectProperty
        -- rdfs.domain ->- subClass
        -- rdfs.range ->- superClass
        -- rdfs.label ->- label).graph
      addTriples(graph, newGraph.triples)
    }
  }

  /** read CSV file with columns owl:Class & rdfs:subClassOf : inheritances to transform */
  def readCSVFile(tableClassesFile: String): List[(Rdf#URI, Rdf#URI)] = {
    val z = run( new FileInputStream(tableClassesFile), URI("urn:/owl/transform"), List() )
    
    // TODO
    List()
  }
}