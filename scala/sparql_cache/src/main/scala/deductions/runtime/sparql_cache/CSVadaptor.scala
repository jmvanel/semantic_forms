package deductions.runtime.sparql_cache

import deductions.runtime.connectors.CSVImporter
import deductions.runtime.core.HTTPrequest
import org.w3.banana.RDF
import scala.util.Try
import org.apache.http.client.methods.HttpGet
import scala.util.Failure
import scala.util.Success
import org.apache.http.impl.client.cache.CachingHttpClientBuilder
import org.apache.http.impl.client.LaxRedirectStrategy


trait CSVadaptor[Rdf <: RDF, DATASET] extends CSVImporter[Rdf, DATASET] {

  import ops._

  def readCSVfromURL(uri: Rdf#URI, contentType: String, dataset: DATASET,
    requestSF: HTTPrequest): Try[Rdf#Graph] = {

    //        setTimeoutsFromConfig()
    val httpClient = CachingHttpClientBuilder.create()
      .setRedirectStrategy(new LaxRedirectStrategy())
      //      .setDefaultRequestConfig(requestConfig)
      .build()
    val request = new HttpGet(fromUri(uri))

    val tryResponse = Try {
      httpClient.execute(request)
    }

    tryResponse match {
      case Failure(f) => Failure(f)
      case Success(response) =>

        val in = response.getEntity.getContent

        val uriPrefix = createUniqueLDPpath(uri, requestSF)
        val graph: Try[Rdf#Graph] = Try {
          run(
            in, uriPrefix
          /* property Value pair to add For Each Row */
          // propertyValueForEachRow // List[(Rdf#URI, Rdf#Node)] = List(),
          //      separator: Char = ','
          )
        }
        logger.info(s"readCSVfromURL graph $graph")
        return graph
    }
  }

  /** create unique LDP path from given uri */
  private def createUniqueLDPpath(uri: Rdf#URI, request: HTTPrequest) = {
    val jURI = new java.net.URI(fromUri(uri))
    URI(
      request.host +
      "/ldp/" +
      jURI.getHost +
      jURI.getPath +
      // jURI.getQuery +
      "#"
    )
  }
}
