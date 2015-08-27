package deductions.runtime.services

import java.io.StringReader

import scala.util.Success
import scala.util.Try

import org.w3.banana.RDF
import org.w3.banana.io.JsonLd
import org.w3.banana.io.JsonLdCompacted
import org.w3.banana.io.RDFReader
import org.w3.banana.io.RDFWriter
import org.w3.banana.io.Turtle

import deductions.runtime.dataset.RDFStoreLocalProvider

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
    extends RDFStoreLocalProvider[Rdf, DATASET]
    with SPARQLHelpers[Rdf, DATASET] {

  val turtleWriter: RDFWriter[Rdf, Try, Turtle]
  implicit val jsonldCompactedWriter: RDFWriter[Rdf, Try, JsonLdCompacted]
  implicit val turtleReader: RDFReader[Rdf, Try, Turtle]
  implicit val jsonldReader: RDFReader[Rdf, Try, JsonLd]

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
  }

  private def makeQueryString(search: String): String =
    s"""
         |CONSTRUCT { ?s ?p ?o } WHERE {
         |  graph <$schemeName:$search> {
         |    ?s ?p ?o .
         |  }
         |}""".stripMargin

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
