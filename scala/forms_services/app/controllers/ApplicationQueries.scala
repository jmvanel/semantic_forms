package controllers.semforms.services

import scala.concurrent.ExecutionContext.Implicits.global
import org.w3.banana.jena.Jena
import deductions.runtime.jena.RDFStoreObject
import deductions.runtime.services.BrowsableGraph
import deductions.runtime.services.StringSearchSPARQL
import play.api.mvc.Action
import play.api.mvc.Controller
import org.w3.banana.RDF
import deductions.runtime.sparql_cache.RDFCacheAlgo
import com.hp.hpl.jena.query.Dataset
import org.w3.banana.jena.JenaModule
import deductions.runtime.jena.JenaHelpers
import deductions.runtime.jena.RDFStoreLocalJena1Provider

/** NOTE: important that JenaModule is first; otherwise ops may be null */
object ApplicationQueries extends JenaModule
  with JenaHelpers
  with RDFStoreLocalJena1Provider
  with ApplicationQueriesTrait[Jena, Dataset]


trait ApplicationQueriesTrait[Rdf <: RDF, DATASET] extends Controller
  with ApplicationCommons
  with RDFCacheAlgo[Rdf, DATASET]
  with StringSearchSPARQL[Rdf, DATASET]
  with BrowsableGraph[Rdf, DATASET]
{

	lazy val dl =  this // new BrowsableGraph()
	// TODO use inverse Play's URI API
	val hrefDisplayPrefix = "/display?displayuri="
    
	def wordsearch(q: String = "") = Action.async {
    val resFuture = searchString(q, hrefDisplayPrefix)
    resFuture .map { res => Ok( res ) }
  }

  def download(url: String) = {
    Action { Ok( // download As String
    		dl.focusOnURI(url) ).as("text/turtle") }
  }
  
  def sparql(query: String) = {
    Action { implicit request =>
      println("sparql: " + request)
      println("sparql: " + query)
      Ok( dl.sparqlConstructQuery(query) )
    }
  }
}
