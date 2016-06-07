package deductions.runtime.sparql_cache.algos

import java.io.FileReader

import org.w3.banana.OWLPrefix
import org.w3.banana.RDF
import org.w3.banana.RDFOps
import org.w3.banana.RDFPrefix
import org.w3.banana.RDFSPrefix
import org.w3.banana.jena.Jena
import org.w3.banana.jena.JenaModule
	import scala.collection.immutable.ListMap

object DuplicatesDetectionOWLGroupBy extends App with JenaModule with DuplicatesDetectionOWL[Jena] {
	val owlFile = args(0)
  val graph = turtleReader.read( new FileReader(owlFile), "") .get
  val datatypePropertiesURI = findDataProperties(graph)
  val datatypePropertiesgroupedByRdfsLabel0 = datatypePropertiesURI.groupBy { n => rdfsLabel( n, graph) }
	val datatypePropertiesgroupedByRdfsLabel = ListMap( datatypePropertiesgroupedByRdfsLabel0.toSeq.sortBy(_._1):_* )
	val report = datatypePropertiesgroupedByRdfsLabel . map {
	  labelAndList => s"'${labelAndList._1}'\n" +
			  (labelAndList._2) . map { n => abbreviateURI(n) } . sorted . mkString("\t", "\n\t", "" )
	} . mkString( "\n" )
	
	output( s"datatypePropertiesgroupedByRdfsLabel\n$report" )
}

/** This App oututs too much : count n*(n-1)/2 ;
 *  rather use DuplicatesDetectionOWLGroupBy */
object DuplicatesDetectionOWLApp extends App with JenaModule with DuplicatesDetectionOWL[Jena] {
  val owlFile = args(0)
  val graph = turtleReader.read( new FileReader(owlFile), "") .get
  val duplicates = findDuplicateDataProperties(graph)
  output( s"duplicates size ${duplicates.duplicates.size}\n")

  val v = duplicates.duplicates.map { dup => dup toString(graph) }
  output( v . mkString("\n") )
  output( s"duplicates size ${duplicates.duplicates.size}")
}


trait DuplicatesDetectionOWL[Rdf <: RDF]
extends DuplicatesDetectionBase[Rdf] {
  /** you can set your own ontology Prefix, that will be replaced on output by ":" */
  val ontologyPrefix = "http://data.onisep.fr/ontologies/"

  implicit val ops: RDFOps[Rdf]
  import ops._
  
  /** @return the list of pairs of similar property URI's */
  def findDuplicateDataProperties(graph: Rdf#Graph): DuplicationAnalysis = {
    val datatypePropertiesURI = findDataProperties(graph)
    val datatypePropertiesPairs = datatypePropertiesURI.toSet.subsets(2).toList
    output(s"datatype Properties pairs count n*(n-1)/2 = ${datatypePropertiesPairs.size}")
    val pairs = for {
      pair <- datatypePropertiesPairs
      pairList: List[Rdf#Node] = pair.toList
      datatypeProperty1 :: datatypeProperty2 :: rest = pairList if (
    		  nodesAreSimilar(datatypeProperty1, datatypeProperty2, graph) )
      //        _ = log(s"pair $pair")
    } yield Duplicate(datatypeProperty1, datatypeProperty2)

    DuplicationAnalysis(pairs.toList)
  }

}
