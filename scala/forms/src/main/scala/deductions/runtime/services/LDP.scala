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
import deductions.runtime.utils.HTTPrequest
import org.w3.banana.io.RDFXML
import deductions.runtime.utils.URIManagement

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
  val rdfXMLWriter: RDFWriter[Rdf, Try, RDFXML]

  implicit val turtleReader: RDFReader[Rdf, Try, Turtle]
  implicit val jsonldReader: RDFReader[Rdf, Try, JsonLd]

  import ops._
  import sparqlOps._
  import rdfStore.transactorSyntax._
  import rdfStore.sparqlEngineSyntax._

  val schemeName = "ldp:"

  /**
   * for LDP GET
   *  @param uri relative URI received by LDP GET
   */
  def getTriples(uri: String, rawURI: String, accept: String, request: HTTPrequest): String = {
    println(s"LDP GET: (uri <$uri>, rawURI <$rawURI>, request $request)")
    val queryString = makeQueryString(uri, rawURI, request)
    println("LDP GET: queryString\n" + queryString)
    val r = rdfStore.r( dataset, {
      for {
        graph <- sparqlConstructQuery(queryString)
        s <- {
          val writer = getWriterFromMIME(accept)
          writer.asString(graph, uri)
        }
      } yield s
    })
    r.get.get
  }

  /** TODO reuse */
  private def getWriterFromMIME(accept: String): RDFWriter[Rdf, Try, _] =
    if (accept == "text/turtle")
      turtleWriter
    else if (accept == "application/rdf+xml")
      rdfXMLWriter
    else // application/ld+json
      jsonldCompactedWriter
            
//  private def absoluteURL( request: HTTPrequest, rawURI: String, secure: Boolean = false): String =
//      "http" + (if (secure) "s" else "") + "://" + request.host + rawURI // + this.appendFragment

  private def makeQueryString(uri: String, rawURI: String, request: HTTPrequest): String = {
		  println(s"makeQueryString rawURI $rawURI")
		  val absoluteURI = request . absoluteURL(rawURI)
      s"""
         |CONSTRUCT { <$absoluteURI> ?p ?o } WHERE {
         |  GRAPH ?G {
         |    <$absoluteURI> ?p ?o .
         |  }
         |}""".stripMargin
  }

  private def makeQueryStringOld(uri: String, rawURI: String): String = {
    println(s"makeQueryString rawURI $rawURI")
    if ( ! rawURI.startsWith( schemeName )) {
      // URI created with forms engine
      println("! rawURI.startsWith( schemeName )")
      val absoluteURI = makeURIFromString(uri)
      s"""
         |CONSTRUCT { <$absoluteURI> ?p ?o } WHERE {
         |  GRAPH ?G {
         |    <$absoluteURI> ?p ?o .
         |  }
         |}""".stripMargin

    } else
      // URI created with LDP POST
      s"""
         |CONSTRUCT { ?s ?p ?o } WHERE {
         |  graph <$schemeName$uri> {
         |    ?s ?p ?o .
         |  }
         |}""".stripMargin
  }

  /** for LDP PUT
   *  TODO content type negotiation is done elsewhere */
  def putTriples(uri: String, link: Option[String], contentType: Option[String],
    slug: Option[String],
    content: Option[String], request: HTTPrequest): Try[String] = {
    val putURI = schemeName + uri + slug.getOrElse("unnamed")

    println(s"putTriples: content: ${content.get}")
    println(s"putTriples: contentType: ${contentType}")
    println(s"putTriples: slug: ${slug}")
    val r = rdfStore.rw( dataset, {
      val reader =
          if (contentType.get.contains("text/turtle"))
            turtleReader
          else jsonldReader
      for {
        graph <- reader.read(new StringReader(content.get), putURI)
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
    })
    println("putTriples: transaction result " + r)
    //    r.flatMap{ res:Failure[ Try[Unit]](err) => Success(putURI)}
    //    r.flatMap{ case res:Failure[Try[Unit]](err) => Success(putURI)}
    Success(putURI)
  }
}
