package deductions.runtime.sparql_cache

import org.w3.banana.RDF
import deductions.runtime.utils.RDFHelpers
import java.net.URL
import org.w3.banana.io.RDFLoader
import scala.util.Try
import org.w3.banana.RDFOps
import org.w3.banana.MGraphOps
import org.w3.banana.RDFOpsModule
import deductions.runtime.dataset.RDFStoreLocalProvider

/**
 * @author jmv
 */
trait DataSourceManager[Rdf <: RDF, DATASET]
    extends RDFStoreLocalProvider[Rdf, DATASET]
    with RDFHelpers[Rdf]
//    with RDFLoader[Rdf, Try]
{
  implicit val rdfLoader: RDFLoader[Rdf, Try]

  import ops._
  import rdfStore.transactorSyntax._
  import rdfStore.graphStoreSyntax._
  import rdfStore.sparqlEngineSyntax._
  import scala.concurrent.ExecutionContext.Implicits.global

  /**
   * replace Same Language Triples in named graph `graphURI`
   *  with the triples coming from given `url`
   *
   *  USE CASE: replace some rdfs:label's of an ontology,
   *  to change the generated forms.
   *  @return number of triples changed
   */
  def replaceSameLanguageTriples(url: URL, graphURIString: String)
    (implicit graph: Rdf#Graph)
    : Int = {
    val r1 = rdfStore.rw( dataset, {
      val graphURI = URI(graphURIString)
      for (givenGraph <- rdfStore.getGraph( dataset, graphURI)) yield {
        val ops1: RDFOps[Rdf]= ops
        val mgraph = givenGraph.makeMGraph()
        val loadedGraph: Rdf#Graph = rdfLoader.load(url).get
        val triples = ops.getTriples(loadedGraph)
        val r = triples.map { replaceSameLanguageTriple( _, mgraph) }
        val total = r.fold(0)(_ + _)
        val modifiedGraph = mgraph.makeIGraph()
        rdfStore.removeGraph( dataset, graphURI)
        rdfStore.appendToGraph( dataset, graphURI, modifiedGraph)
        total
      }
    })
    r1.get.get
  }
}