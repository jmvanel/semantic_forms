package deductions.runtime.services

import scala.util.Try
import org.w3.banana.RDF
import org.w3.banana.SparqlOpsModule
import deductions.runtime.dataset.RDFStoreLocalProvider
import org.w3.banana.TurtleWriterModule
import org.w3.banana.TurtleReaderModule
import java.io.StringReader
import scala.util.Success

/**
 * A simple (partial) LDP implementation backed by SPARQL
 * http://www.w3.org/TR/ldp-primer/#creating-an-rdf-resource-post-an-rdf-resource-to-an-ldp-bc
 *
 * POST /alice/ HTTP/1.1
 * Host: example.org
 * Link: <http://www.w3.org/ns/ldp#Resource>; rel="type"
 * Slug: foaf
 * Content-Type: text/turtle
 *
 *
 * GET /alice/ HTTP/1.1
 * Host: example.org
 * Accept: text/turtle
 *
 * @author jmv
 */
trait LDP[Rdf <: RDF, DATASET]
    extends SparqlOpsModule
    with RDFStoreLocalProvider[Rdf, DATASET]
    with TurtleWriterModule
    with TurtleReaderModule {

  import ops._
  import sparqlOps._
  import rdfStore.transactorSyntax._
  import rdfStore.sparqlEngineSyntax._

  def makeQueryString(search: String): String =
    s"""
         |SELECT DISTINCT ?thing WHERE {
         |  graph <$search> {
         |    ?s ?p ?o .
         |  }
         |}""".stripMargin

  /** for LDP GET */
  def getTriples(uri: String, accept: String): String = {
    val r = dataset.r {
      for {
        graph <- sparqlConstructQuery(makeQueryString(uri))
        s <- turtleWriter.asString(graph, uri)
      } yield s
    }
    r.get.get
  }

  /** NON transactional */
  def sparqlConstructQuery(queryString: String): Try[Rdf#Graph] = {
    for {
      query <- parseConstruct(queryString) // .asFuture
      es <- dataset.executeConstruct(query, Map())
    } yield es
  }

  /** for LDP PUT */
  def putTriples(uri: String, link: Option[String], contentType: Option[String],
    slug: Option[String],
    content: Option[String]): Try[String] = {
    val putURI = uri + slug.getOrElse("unnamed")
    println(s"content: ${content.get}")
    println(s"contentType: ${contentType}")
    dataset.rw {
      for {
        graph <- turtleReader.read(new StringReader(content.get), putURI)
        res <- {
          println("graph: " + graph);
          rdfStore.removeGraph(dataset, URI(putURI))
        }
        res2 <- rdfStore.appendToGraph(dataset, URI(putURI), graph)
      } yield res2
    }
    Success(putURI)
  }
}