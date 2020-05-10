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

import scalaz._
import Scalaz._

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

  val defaultContentType = jsonldMime

  val turtleWriter: RDFWriter[Rdf, Try, Turtle]
  implicit val jsonldCompactedWriter: RDFWriter[Rdf, Try, JsonLdCompacted]
  val rdfXMLWriter: RDFWriter[Rdf, Try, RDFXML]

  implicit val turtleReader: RDFReader[Rdf, Try, Turtle]
  implicit val jsonldReader: RDFReader[Rdf, Try, JsonLd]

	val defaultLDPheaders = Seq(
    // DEBUG for yannick TODO reestablish !!!!!! ACCESS_CONTROL_ALLOW_ORIGIN -> "*",
    "Link" -> """<http://www.w3.org/ns/ldp#BasicContainer>; rel="type", <http://www.w3.org/ns/ldp#Resource>; rel="type"""",
    "Allow" -> "OPTIONS,GET,POST,PUT,PATCH,HEAD",
    "Accept-Post" -> """"text/turtle, application/ld+json"""
  )

  import ops._

  val servicePrefix = "/ldp/"

  /**
   * for LDP GET , getting a RDF resource or container
   *  @param uri relative URI received by LDP GET
   *  @param relativeURIpath relative URI path, actually request.path
   *  TODO @return Try[String]
   */
  def getTriples(uri: String, relativeURIpath: String, accept: String, request: HTTPrequest): String = {
    val triplesResource = getTriplesNonContainer(uri, relativeURIpath, accept, request)
    logger.info(s"""getTriples(<$uri>: triplesResource (triplesNonContainer) "$triplesResource" """)
//    println( """triplesNonContainer.startsWith("{ }") """ + triplesResource.startsWith("{ }") )
    // TODO getTriplesNonContainer() should return a graph , not a string
    val acceptHeader = request.getHTTPheaderValue("Accept")
    val triplesResourceNonEmpty =
      acceptHeader match {
        case Some(`turtleMime`) =>
//        println( s"Some(turtleMime)")
        (triplesResource =/= "")
        case Some(`jsonldMime`) =>
//        println( s"Some(jsonldMime)")
          ! triplesResource.startsWith("{ }")
        case Some(`rdfXMLmime`) => triplesResource.size > 150
        case Some(_) =>
//          println( s"Some(_)")
          ! triplesResource.startsWith("{ }")
        case None => ! triplesResource.startsWith("{ }") // JSON-LD is the default MIME
      }
//    println( s"""acceptHeader $acceptHeader , triplesResourceNonEmpty $triplesResourceNonEmpty""")
    if ( triplesResourceNonEmpty )
      triplesResource
    else {
      val listContainerResult = listContainer(
        uri,
        request.getHTTPparameterValue("Link"),
        request.getHTTPparameterValue("Content-Type"))
      listContainerResult match {
        case Success(s) => s
        case Failure(f) => f.toString()
      }
    }
  }

  /**
   * for LDP GET , getting a RDF container
   *  @param uri relative URI received by LDP GET
   *  @param relativeURIpath relative URI path, actually request.path
   */
  private def getTriplesNonContainer(uri: String, relativeURIpath: String, accept: String, request: HTTPrequest): String = {
    logger.info(s"LDP GET: (uri <$uri>, rawURI <$relativeURIpath>, request $request)")
    val queryString = makeQueryString(uri, relativeURIpath, request)
    logger.info("LDP GET: queryString\n" + queryString)
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
   *  and also with plain SF forms on local data.
   *  @param relativeURIpath relative URI path, actually request.path
   *  */
  private def makeQueryString(uri: String, relativeURIpath: String, request: HTTPrequest): String = {
		  logger.info(s"makeQueryString rawURI $relativeURIpath")
		  val absoluteURI = request . absoluteURL(relativeURIpath)
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
    val graphTry = rdfStore.r( dataset, {
    val urisWithGivenPrefixRaw = sparqlSelectQueryVariablesNT(
      /* match e.g.
        http://semantic-forms.cc:9111/ldp/yannick/fludy/d4a6350c81.ttl
        but not
        http://localhost:9000/ldp/1513608041265-77442744222862
       */
//      namedGraphs(regex = Some(s"""$uri/\\w+/.*""")),
      namedGraphs(regex = Some(s"""$uri""")),
      Seq("?thing"))
    val urisWithGivenPrefix = urisWithGivenPrefixRaw.flatten.map {
      uri => nodeToString(uri)
    }
    val directPathChildren =  filterDirectPathChildren(uri, urisWithGivenPrefix)
    logger.info(s">>>> directPathChildren $directPathChildren")
    val triples = directPathChildren . map {
      child => Triple( URI(uri), URI(ldpPrefix+"contains"), URI(child))
    }
    makeGraph(
        Triple( URI(uri), rdf.typ, URI(ldpPrefix+"Container")) ::
        triples )

    })

    //  TODO use conneg helper
    turtleWriter.asString(graphTry.getOrElse(emptyGraph), base = uri)

    /* # EXAMPLE from LDP primer
     * @prefix ldp: <http://www.w3.org/ns/ldp#>.
     * <http://example.org/alice/> a ldp:Container, ldp:BasicContainer;
     * ldp:contains XXX */

    // TODO put this in reuable function
//    tryString . match {
//      case Success(s) => s
//      case Failure(f) => f.toString
//    }
  }

  private def filterDirectPathChildren(parent: String, paths: List[String]): List[String] = {
    logger.info(s"filterDirectPathChildren(parent=$parent")
    val regexChildrenContainer = (s".*/$parent" + """/([\w\.\-]+)/.*""").r
    logger.info(s"""filterDirectPathChildren
      regexChildrenContainer $regexChildrenContainer,
      paths ${paths.mkString(", ")}""")
    paths.collect(path => {
      path match {
        case regexChildrenContainer(child) =>
          logger.info(s"regexChildrenContainer path ${path} child $child")
          child

         // actually this is processed in getTriplesNonContainer()
        case _ if path.endsWith(parent) =>
          logger.info(s"path.endsWith(parent)")
          ""  
        case _ =>
          logger.info(s"filterDirectPathChildren NO! path ${path}")
          ""
      }
    })
  }

  /** for LDP PUT or POST */
  def putTriples(uri: String, link: Option[String], contentType: Option[String],
    slug: Option[String],
    content: Option[String], request: HTTPrequest): Try[String] = {
    val putURI = request.absoluteURL(
      servicePrefix + uri +
      ( if( uri === "" || uri.endsWith("/") ) "" else "/" ) +
      slug.getOrElse( makeId("") ) )

    logger.info(s"putTriples: content: ${content.get}")
    logger.info(s"""putTriples: contentType: "${contentType}""")
    logger.info(s"""putTriples: slug: "${slug}"""")
    logger.info(s"""putTriples: uri (path): "${uri}"""")
    logger.info(s"putTriples: PUT (graph) URI: ${putURI}")

    val r = rdfStore.rw( dataset, {
   // TODO content type negotiation is done elsewhere
   val reader =
          if (contentType.get.contains("text/turtle"))
            turtleReader
          else jsonldReader
      val resFor = for {
        graph <- reader.read(new StringReader(content.get), putURI)
        res <- {
          logger.info("putTriples: before removeGraph: graph: " + graph);
          rdfStore.removeGraph(dataset, URI(putURI))
        }
        res2 <- {
          logger.info("putTriples: appendToGraph: URI= " + putURI);
          val res = rdfStore.appendToGraph(dataset, URI(putURI), graph)
          logger.info("putTriples: after appendToGraph: URI= " + putURI)
          res
        }
      } yield res2
    })
    logger.info("putTriples: transaction result " + r)
    Success(putURI)
  }

  /** for LDP DELETE */
  def deleteResource(uri: String, request: HTTPrequest): Try[String] = {
    if (uri.matches(s"${(".*/.*")}")) {
      val r = rdfStore.rw(dataset, {
        val graphURI = request.absoluteURL("/ldp/"+uri)
        logger.info( s"deleteResource: remove Graph $graphURI")
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
