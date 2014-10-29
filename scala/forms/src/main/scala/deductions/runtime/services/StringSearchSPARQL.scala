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
import deductions.runtime.html.Form2HTML
import scala.concurrent.duration._
import org.apache.log4j.Logger

/** String Search with simple SPARQL */
class StringSearchSPARQL[Rdf <: RDF](store: RDFStore[Rdf])(
  implicit //    reader: RDFReader[Rdf, RDFXML],
  ops: RDFOps[Rdf],
  sparqlOps: SparqlOps[Rdf]) {

  import ops._
  import sparqlOps._

  val sparqlEngine = SparqlEngine[Rdf](store)

//  def search(search: String, hrefPrefix:String="") : Elem = {
//    val uris = search_only(search)
//    val res = Await.result(uris, 5000 millis)
//    Logger.getRootLogger().info(s"search $search ${res.mkString(", ")}")
//    displayResults( res, hrefPrefix)
//  }

  def search(search: String, hrefPrefix:String="") : Future[Elem] = {
    val uris = search_only(search)
    val elem = uris . map (
        (u : Iterable[Rdf#Node]) =>
          displayResults( u, hrefPrefix) )
    elem
  }
    
  private def displayResults( res: Iterable[Rdf#Node], hrefPrefix:String ) = {
    <p>{
      res.map( uri => {
        val uriString = uri.toString
        val blanknode = ! ops.isURI(uri)
        <div><a href={Form2HTML.createHyperlinkString( hrefPrefix, uriString, blanknode) }>
        { uriString }</a><br/></div>
      } )
    }</p>
  }
  
  private def search_only(search: String) = {
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
    uris
  }

}