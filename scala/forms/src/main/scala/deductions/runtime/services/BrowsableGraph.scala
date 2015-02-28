package deductions.runtime.services

import java.io.ByteArrayOutputStream

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.Try

import org.apache.log4j.Logger
import org.w3.banana.RDF
import org.w3.banana.RDFOps
import org.w3.banana.RDFStore
import org.w3.banana.SparqlEngine
import org.w3.banana.SparqlOps
import org.w3.banana.TryW
import org.w3.banana.io.RDFWriter
import org.w3.banana.io.Turtle

import deductions.runtime.jena.RDFStoreObject

/**
 * Browsable Graph implementation, in the sense of
 *  http://www.w3.org/DesignIssues/LinkedData.html
 */
class BrowsableGraph[Rdf <: RDF]()(
    implicit ops: RDFOps[Rdf],
    sparqlOps: SparqlOps[Rdf],
    turtleWriter: RDFWriter[Rdf, Try, Turtle], rdfStore: RDFStore[Rdf, Try, RDFStoreObject.DATASET]) {

  import ops._
  import sparqlOps._
  import rdfStore.sparqlEngineSyntax._
  val dataset = RDFStoreObject.dataset

  /**
   * all triples in graph <search> , plus "reverse" triples everywhere
   *  used in Play! app : NON blocking !
   */
  def search_only(search: String): Future[Rdf#Graph] = {
    val queryString =
      s"""
         |CONSTRUCT {
         |  ?thing ?p ?o .
         |  ?s ?p1 ?thing .     
         |}
         |WHERE {
         |  { graph <$search>
         |    { ?thing ?p ?o . }
         |  } OPTIONAL {
         |  graph ?GRAPH
         |   { ?s ?p1 ?thing . }
         |  }
         |}""".stripMargin
    println("search_only " + queryString)
    sparqlConstructQueryFuture(queryString)
  }

  def sparqlConstructQueryFuture(queryString: String): Future[Rdf#Graph] = {
    val r = for {
      query <- parseConstruct(queryString) // .asFuture
      es <- dataset.executeConstruct(query, Map())
    } yield es
    r.asFuture
  }

  /** not used in Play! app : blocking ! */
  def focusOnURI(uri: String): String = {
    val triples = search_only(uri)
    futureGraph2String(triples, uri)
  }

  private def futureGraph2String(triples: Future[Rdf#Graph], uri: String): String = {
    val graph = Await.result(triples, 5000 millis)
    Logger.getRootLogger().info(s"uri $uri ${graph}")
    val to = new ByteArrayOutputStream
    val ret = turtleWriter.write(graph, to, base = uri)
    to.toString
  }

  def sparqlConstructQuery(queryString: String): String = {
    val r = sparqlConstructQueryFuture(queryString)
    futureGraph2String(r, "")
  }

}