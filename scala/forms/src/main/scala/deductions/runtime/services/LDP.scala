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
    with SPARQLHelpers[Rdf, DATASET]
    with URIManagement {

  val turtleWriter: RDFWriter[Rdf, Try, Turtle]
  implicit val jsonldCompactedWriter: RDFWriter[Rdf, Try, JsonLdCompacted]
  implicit val turtleReader: RDFReader[Rdf, Try, Turtle]
  implicit val jsonldReader: RDFReader[Rdf, Try, JsonLd]

  import ops._
  import sparqlOps._
  import rdfStore.transactorSyntax._
  import rdfStore.sparqlEngineSyntax._

  val schemeName = "lpd:"

  /** for LDP GET
   *  @param uri relative URI received by LDP GET */
  def getTriples(uri: String, rawURI: String, accept: String): String = {
    println(s"LDP GET: (uri <$uri>, rawURI <$rawURI>)")
    println("LDP GET:\n" + makeQueryString(uri, rawURI))
    val r = dataset.r {
      for {
        graph <- sparqlConstructQuery(makeQueryString(uri, rawURI))
        s <- if (accept == "text/turtle")
          turtleWriter.asString(graph, uri)
        else
          jsonldCompactedWriter.asString(graph, uri)
      } yield s
    }
    r.get.get
  }

  private def makeQueryString(uri: String, rawURI: String): String = {
    if ( true) // rawURI.endsWith(uri))
      // URI created with forms engine
      s"""
         |CONSTRUCT { <${makeURIFromString(uri)}> ?p ?o } WHERE {
         |  GRAPH ?G {
         |    <${makeURIFromString(uri)}> ?p ?o .
         |  }
         |}""".stripMargin
    else
      // URI created with LDP POST
      s"""
         |CONSTRUCT { ?s ?p ?o } WHERE {
         |  graph <$schemeName$uri> {
         |    ?s ?p ?o .
         |  }
         |}""".stripMargin
    //    s"""
    //         |CONSTRUCT { ?s ?p ?o } WHERE {
    //         |  {
    //         |  graph <$schemeName$uri> {
    //         |    ?s ?p ?o .
    //         |  }
    //         |  } UNION {
    //         |  GRAPH ?G {
    //         |    # <${makeURIFromString(uri)}> ?p ?o .
    //         |    <${uri}> ?p ?o .
    //         |  }
    //         |  }
    //         |}""".stripMargin
  }

  /** for LDP PUT */
  def putTriples(uri: String, link: Option[String], contentType: Option[String],
    slug: Option[String],
    content: Option[String]): Try[String] = {
    val putURI = schemeName + uri + slug.getOrElse("unnamed")

    println(s"putTriples: content: ${content.get}")
    println(s"putTriples: contentType: ${contentType}")
    println(s"putTriples: slug: ${slug}")
    val r = dataset.rw {
      for {
        graph <- if (contentType.get.contains("text/turtle"))
          turtleReader.read(new StringReader(content.get), putURI)
        else
          jsonldReader.read(new StringReader(content.get), putURI)
        res <- {
          println("putTriples: graph: " + graph);
          rdfStore.removeGraph(dataset, URI(putURI))
        }
        res2 <- {
          println("putTriples: appendToGraph: " + putURI);
          val res = rdfStore.appendToGraph(dataset, URI(putURI), graph)
          println("putTriples: after appendToGraph: " + putURI)
          res
        }
      } yield res2
    }
    println("putTriples: transaction result " + r)
    //    r.flatMap{ res:Failure[ Try[Unit]](err) => Success(putURI)}
    //    r.flatMap{ case res:Failure[Try[Unit]](err) => Success(putURI)}
    Success(putURI)
  }
}
