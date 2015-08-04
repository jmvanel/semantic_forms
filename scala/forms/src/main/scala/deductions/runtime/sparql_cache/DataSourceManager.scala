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
    with RDFLoader[Rdf, Try]
    with RDFOpsModule {
  import ops._
  import rdfStore.transactorSyntax._
  import rdfStore.graphStoreSyntax._
  import rdfStore.sparqlEngineSyntax._
  import scala.concurrent.ExecutionContext.Implicits.global

  /**
   * replace Same Language Triples in named graph `graphURI`
   *  with the triples coming from given `url`
   *  USE CASE: replace some rdfs:label's of an ontology,
   *  to change the generated forms.
   *  @return number of triples changed
   */
  def replaceSameLanguageTriples(url: URL, graphURIString: String): Int = {
    val r1 = dataset.rw({
      val graphURI = URI(graphURIString)
      for (givenGraph <- dataset.getGraph(graphURI)) yield {
        val rdfh: RDFHelpers[Rdf] = new RDFHelpers[Rdf] { val graph = givenGraph }
        val mgraph = givenGraph.makeMGraph()
        /* TODO check new versions of Scala > 2.11.6 that this asInstanceOf is 
        still necessary */
        val loadedGraph: Rdf#Graph = load(url).get.
          asInstanceOf[Rdf#Graph]
        val triples = ops.getTriples(loadedGraph)
        val r = triples.map {
          t => rdfh.replaceSameLanguageTriple(t, mgraph)
        }
        val total = r.fold(0)(_ + _)
        val modifiedGraph = mgraph.makeIGraph()
        dataset.removeGraph(graphURI)
        dataset.appendToGraph(graphURI, modifiedGraph)
        total
      }
    })
    r1.get.get
  }
}