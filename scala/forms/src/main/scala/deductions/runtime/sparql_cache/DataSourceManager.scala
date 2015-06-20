package deductions.runtime.sparql_cache

import org.w3.banana.RDF
import deductions.runtime.utils.RDFHelpers
import java.net.URL
import org.w3.banana.io.RDFLoader
import scala.util.Try
import org.w3.banana.RDFOps
import org.w3.banana.MGraphOps

/**
 * @author jmv
 */
trait DataSourceManager[Rdf <: RDF, DATASET]
    extends RDFHelpers[Rdf]
    with RDFLoader[Rdf, Try]
    with RDFOps[Rdf]
    with MGraphOps[Rdf] {

  /**
   * replace Same Language Triples from given URL
   *  into current graph
   */
  def replaceSameLanguageTriples(url: URL): Unit = {
    val graf = load(url).get
    val triples = getTriples(graf)
    triples.map {
      t => replaceSameLanguageTriple(t)
    }
  }
}