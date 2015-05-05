package controllers.semforms.services

import scala.concurrent.ExecutionContext.Implicits.global

import org.w3.banana.jena.Jena

import deductions.runtime.jena.RDFStoreObject
import deductions.runtime.services.BrowsableGraph
import deductions.runtime.services.StringSearchSPARQL
import deductions.runtime.sparql_cache.RDFCache
import play.api.mvc.Action
import play.api.mvc.Controller

object ApplicationQueries extends Controller with ApplicationCommons
      with RDFCache
      with StringSearchSPARQL[Jena, RDFStoreObject.DATASET] {

	lazy val dl = new BrowsableGraph()
	// TODO use inverse Play's URI API
	val hrefDisplayPrefix = "/display?displayuri="
    
	def wordsearch(q: String = "") = Action.async {
    val resFuture = search(q, hrefDisplayPrefix)
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