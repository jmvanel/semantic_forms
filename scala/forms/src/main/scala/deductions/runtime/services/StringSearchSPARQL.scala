package deductions.runtime.services

import org.w3.banana.SparqlEngine
import org.w3.banana.RDF
import org.w3.banana.RDFOps
import org.w3.banana.SparqlOps
import org.w3.banana.RDFStore
import scala.xml.Elem
import scala.concurrent._
import scala.concurrent.util._
import scala.concurrent.ExecutionContext.Implicits.global

/** String Search with simple SPARQL */
class StringSearchSPARQL[Rdf <: RDF](store: RDFStore[Rdf])(
  implicit //    reader: RDFReader[Rdf, RDFXML],
  ops: RDFOps[Rdf],
  sparqlOps: SparqlOps[Rdf]) {

  import ops._
  import sparqlOps._

  val sparqlEngine = SparqlEngine[Rdf](store)

  def search(search: String) : Elem = {
    val queryString =
    // |prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
      s"""
         |SELECT DISTINCT ?thing WHERE {
         |  graph ?g {
         |    ?thing ?p ?string .
         |    FILTER regex( ?string, "$search", 'i')
         |  }
         |}""".stripMargin
    val query = SelectQuery(queryString)
    val es = sparqlEngine.executeSelect(query)
    val uris: Future[Iterable[Rdf#Node]] = es.
      map(_.toIterable.map {
        row => row("thing") getOrElse sys.error("")
      })
    <p>{
      uris.map( uri => {
        val uriString = uri.toString
        <div><a href={uriString}>{uriString}</a><br/></div>
      } )
    }</p>
  }
}