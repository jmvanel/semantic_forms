package deductions.runtime.services

import java.io.StringReader

import deductions.runtime.sparql_cache.SPARQLHelpers
import deductions.runtime.utils.RDFStoreLocalProvider
import deductions.runtime.utils.URIManagement
import deductions.runtime.core.HTTPrequest
import org.w3.banana.RDF
import org.w3.banana.io._

import scala.util.{Success, Try}
import scala.util.Failure
import scala.util.matching.Regex
import deductions.runtime.utils.RDFPrefixes

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
    with RDFContentNegociationIO[Rdf, DATASET]
    with NavigationSPARQLBase[Rdf]
    with RDFPrefixes[Rdf]
    with URIManagement {

  val turtleWriter: RDFWriter[Rdf, Try, Turtle]
  implicit val jsonldCompactedWriter: RDFWriter[Rdf, Try, JsonLdCompacted]
  val rdfXMLWriter: RDFWriter[Rdf, Try, RDFXML]

  implicit val turtleReader: RDFReader[Rdf, Try, Turtle]
  implicit val jsonldReader: RDFReader[Rdf, Try, JsonLd]

  import ops._

  val servicePrefix = "/ldp/"

  /**
   * for LDP GET , getting a RDF resource
   *  @param uri relative URI received by LDP GET
   *  TODO document @param rawURI
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

  /** For LDP GET: http://localhost:9000/ldp/a/b/c returns BOTH triples in that named graph,
   *  AND triples with that subject, which is coherent with what LDP PUT does,
   *  and also with plain SF forms on local data. */
  private def makeQueryString(uri: String, rawURI: String, request: HTTPrequest): String = {
		  println(s"makeQueryString rawURI $rawURI")
		  val absoluteURI = request . absoluteURL(rawURI)
      s"""
         |CONSTRUCT {
         |  ?s ?p ?o .
         |  <$absoluteURI> ?p ?o .
         |} WHERE {
         |  {
         |  GRAPH <$absoluteURI> {
         |    ?s ?p ?o .
         |  }
         |  } UNION {
         |  GRAPH ?G {
         |    <$absoluteURI> ?p ?o .
         |  } }
         |}""".stripMargin
  }

  /** for LDP GET , listing of a basic container */
  def listContainer(uri: String, link: Option[String], contentType: Option[String]):
  Try[String] = {
    val ldpPrefix = "http://www.w3.org/ns/ldp#"
    val urisWithGivenPrefixRaw = sparqlSelectQueryVariablesNT(
      /* match e.g.
        http://semantic-forms.cc:9111/ldp/yannick/fludy/d4a6350c81.ttl
        but not
        http://localhost:9000/ldp/1513608041265-77442744222862
       */
      namedGraphs(regex = Some(s"$uri/\w+/.*")),
      Seq("?thing"))
    val urisWithGivenPrefix = urisWithGivenPrefixRaw.flatten.map {
      uri => nodeToString(uri)
    }
    val directPathChildren =  filterDirectPathChildren(uri, urisWithGivenPrefix)
    val triples = directPathChildren . map {
      child => Triple( URI(uri), URI(ldpPrefix+"contains"), URI(child))
    }
    val graph = makeGraph(
        Triple( URI(uri), rdf.typ, URI(ldpPrefix+"Container")) ::
        triples )

    //  TODO use conneg helper
    turtleWriter.asString(graph, base = uri)

    /* # EXAMPLE from LDP primer
     * @prefix ldp: <http://www.w3.org/ns/ldp#>.
     * <http://example.org/alice/> a ldp:Container, ldp:BasicContainer;
     * ldp:contains XXX */

//    tryString . match {
//      case Success(s) => s
//      case Failure(f) => f.toString
//    }
  }

  private def filterDirectPathChildren(parent: String, paths: List[String]): List[String] = {
    val regexDirectPathChildren = (s"$parent/\w+/\w+").r
    for (path <- paths) yield {
      path match {
        case regexDirectPathChildren(_*) => path
      }
    }
  }

  /** for LDP PUT or POST */
  def putTriples(uri: String, link: Option[String], contentType: Option[String],
    slug: Option[String],
    content: Option[String], request: HTTPrequest): Try[String] = {
    val putURI = request.absoluteURL(
      servicePrefix + uri +
      ( if( uri.endsWith("/") ) "" else "/" ) +
      slug.getOrElse( makeId("") ) )

    println(s"putTriples: content: ${content.get}")
    println(s"putTriples: contentType: ${contentType}")
    println(s"putTriples: slug: ${slug}")
    println(s"putTriples: PUT (graph) URI: ${putURI}")

    val r = rdfStore.rw( dataset, {
   // TODO content type negotiation is done elsewhere
   val reader =
          if (contentType.get.contains("text/turtle"))
            turtleReader
          else jsonldReader
      val resFor = for {
        graph <- reader.read(new StringReader(content.get), putURI)
        res <- {
          println("putTriples: before removeGraph: graph: " + graph);
          rdfStore.removeGraph(dataset, URI(putURI))
        }
        res2 <- {
          println("putTriples: appendToGraph: URI= " + putURI);
          val res = rdfStore.appendToGraph(dataset, URI(putURI), graph)
          println("putTriples: after appendToGraph: URI= " + putURI)
          res
        }
      } yield res2
    })
    println("putTriples: transaction result " + r)
    Success(putURI)
  }

  /** for LDP DELETE */
  def deleteResource(uri: String, request: HTTPrequest): Try[String] = {
    if (uri.matches(s"${(".*/.*")}")) {
      val r = rdfStore.rw(dataset, {
        val graphURI = request.absoluteURL("/ldp/"+uri)
        println( s"deleteResource: remove Graph $graphURI")
        rdfStore.removeGraph(dataset, URI(graphURI) )
      })
      r.flatten match {
        case Success(_) => Success(s"deleteResource(uri=$uri): Success")
        case Failure(f) => Failure(new Exception(s"deleteResource(uri=$uri): $f", f))
      }
    } else Failure(new Exception(
      s"deleteResource(uri=$uri): URI looks like a user created data; only possible to LDP DELETE URI's like /ldp/fileOrDir"))
  }
}
