package deductions.runtime.services

import scala.util.Try
import org.w3.banana.RDF
import org.w3.banana.SparqlOpsModule
import deductions.runtime.dataset.RDFStoreLocalProvider
import org.w3.banana.TurtleWriterModule
import org.w3.banana.TurtleReaderModule
import java.io.StringReader
import scala.util.Success
import scala.util.Failure
import org.w3.banana.JsonLDWriterModule
import org.w3.banana.JsonLDReaderModule

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
    with JsonLDWriterModule
    with TurtleReaderModule
    with JsonLDReaderModule {

  import ops._
  import sparqlOps._
  import rdfStore.transactorSyntax._
  import rdfStore.sparqlEngineSyntax._

  val schemeName = "lpd"

  /** for LDP GET */
  def getTriples(uri: String, accept: String): String = {
    println("GET:\n" + makeQueryString(uri))
    val r = dataset.r {
      for {
        graph <- sparqlConstructQuery(makeQueryString(uri))
        s <- if (accept == "text/turtle")
          turtleWriter.asString(graph, uri)
        else
          jsonldCompactedWriter.asString(graph, uri)
      } yield s
    }
    r.get.get
    //    r.flatten
  }

  def makeQueryString(search: String): String =
    s"""
         |CONSTRUCT { ?s ?p ?o } WHERE {
         |  graph <$schemeName:$search> {
         |    ?s ?p ?o .
         |  }
         |}""".stripMargin

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
    val putURI = schemeName + ":" + uri + slug.getOrElse("unnamed")

    println(s"content: ${content.get}")
    println(s"contentType: ${contentType}")
    val r = dataset.rw {
      for {
        graph <- if (contentType.get.contains("text/turtle"))
          turtleReader.read(new StringReader(content.get), putURI)
        else
          jsonldReader.read(new StringReader(content.get), putURI)
        res <- {
          println("graph: " + graph);
          rdfStore.removeGraph(dataset, URI(putURI))
        }
        res2 <- rdfStore.appendToGraph(dataset, URI(putURI), graph)
      } yield res2
    }
    println("putTriples: " + r)
    //    r.flatMap{ res:Failure[ Try[Unit]](err) => Success(putURI)}
    //    r.flatMap{ case res:Failure[Try[Unit]](err) => Success(putURI)}
    Success(putURI)
  }
}
