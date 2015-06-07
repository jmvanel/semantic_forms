package deductions.runtime.services

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.Elem
import org.w3.banana.RDF
import org.w3.banana.SparqlOpsModule
import org.w3.banana.TryW
import org.w3.banana.syntax._
import deductions.runtime.abstract_syntax.InstanceLabelsInference2
import deductions.runtime.dataset.RDFStoreLocalProvider
import deductions.runtime.html.Form2HTML
import org.w3.banana.Transactor

trait SPARQLQueryMaker {
  def makeQueryString(search: String): String
}

/**
 * Generic SPARQL Search with single parameter,
 *  and showing in HTML a column of hyperlinked results with instance Labels
 */
trait ParameterizedSPARQL[Rdf <: RDF, DATASET]
    extends InstanceLabelsInference2[Rdf] with SparqlOpsModule
    with RDFStoreLocalProvider[Rdf, DATASET] {

  import ops._
  import sparqlOps._
  import rdfStore.transactorSyntax._
  import rdfStore.sparqlEngineSyntax._

  def search(search: String, hrefPrefix: String = "")(implicit queryMaker: SPARQLQueryMaker): Future[Elem] = {
    val uris = search_only(search)
    val elem = uris.map(
      u => displayResults(u.toIterable, hrefPrefix))
    elem
  }

  /**
   * CAUTION: It is of particular importance to note that
   *  one should never use an iterator after calling a method on it.
   */
  private def displayResults(res0: Iterable[Rdf#Node], hrefPrefix: String) = {
    <p>{
      val res = res0.toSeq
      println(s"displayResults : ${res.mkString("\n")}")
      dataset.r({
        implicit val graph: Rdf#Graph = allNamedGraph
        res.map(uri => {
          val uriString = uri.toString
          val blanknode = !isURI(uri)
          // TODO : show named graph
          <div title={ uri.toString() }><a href={ Form2HTML.createHyperlinkString(hrefPrefix, uriString, blanknode) }>
                                          { instanceLabel(uri) }
                                        </a><br/></div>
        })
      }).get
    }</p>
  }

  /**
   * NOTE: this stuff is pretty generic;
   *  just add these arguments :
   *  queryString:String, vars:Seq[String]
   *  ; transactional
   */
  private def search_only(search: String)(implicit queryMaker: SPARQLQueryMaker): Future[Iterator[Rdf#Node]] = {
    val queryString = queryMaker.makeQueryString(search)

    val transaction =
      dataset.r({
        val result = for {
          query <- parseSelect(queryString)
          solutions <- dataset.executeSelect(query, Map())
        } yield {
          solutions.toIterable.map {
            row =>
              row("thing") getOrElse sys.error(s"search_only($search) : no ?thing in row")
          }
        }
        result
      })
    val tryIteratorRdfNode = transaction.flatMap { identity }
    tryIteratorRdfNode.asFuture
  }

  def isURI(node: Rdf#Node) = ops.foldNode(node)(identity, x => None, x => None) != None

}