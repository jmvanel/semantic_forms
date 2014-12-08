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
import deductions.runtime.jena.RDFStoreObject
import scala.util.Try
import org.w3.banana._

/** String Search with simple SPARQL */
class StringSearchSPARQL[Rdf <: RDF](
//    store: RDFStore[Rdf, Try, RDFStoreObject.DATASET]
)(
  implicit //    reader: RDFReader[Rdf, RDFXML],
  ops: RDFOps[Rdf],
  sparqlOps: SparqlOps[Rdf]
//  sparqlEngine : SparqlEngine[Rdf, Future, RDFStoreObject.DATASET]
    , rdfStore: RDFStore[Rdf, Try, RDFStoreObject.DATASET]
) {

  import ops._
  import sparqlOps._

  def search(search: String, hrefPrefix:String="") : Future[Elem] = {
    val uris = search_only(search)
    val elem = uris . map (
        u => displayResults( u.toIterable, hrefPrefix) )
    elem
  }
    
  private def displayResults( res: Iterable[Rdf#Node], hrefPrefix:String ) = {
    <p>{
      res.map( uri => {
        val uriString = uri.toString
        val blanknode = ! isURI(uri)
        <div><a href={Form2HTML.createHyperlinkString( hrefPrefix, uriString, blanknode) }>
        { uriString }</a><br/></div>
      } )
    }</p>
  }
  
  private def search_only(search: String) : Future[Iterator[Rdf#Node]]= {
   val queryString =
      s"""
         |SELECT DISTINCT ?thing WHERE {
         |  graph ?g {
         |    ?thing ?p ?string .
         |    FILTER regex( ?string, "$search", 'i')
         |  }
         |}""".stripMargin
    val r = for {
      query <- parseSelect(queryString).asFuture
      solutions <- rdfStore.executeSelect( RDFStoreObject.dataset, query, Map()).
        asFuture
    } yield {
      solutions.toIterable.map {
        row => row("thing") getOrElse sys.error("")
      }      
    }        
    r // toIterable
  }
  
  def isURI(    node:Rdf#Node) = ops.foldNode( node )(identity, x => None, x => None) != None

}