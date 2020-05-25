package deductions.runtime.services

import org.w3.banana.RDF

import scala.concurrent.Future
import scala.xml.NodeSeq
import deductions.runtime.core.HTTPrequest
import deductions.runtime.sparql_cache.RDFCacheAlgo

/** Reverse Links Search with simple SPARQL */
trait ReverseLinksSearchSPARQL[Rdf <: RDF, DATASET]
  extends ParameterizedSPARQL[Rdf, DATASET]
  with StringSearchSPARQLBase[Rdf]
  with NavigationSPARQLBase[Rdf]
  with RDFCacheAlgo[Rdf, DATASET] {

  private implicit val queryMaker = new SPARQLQueryMaker[Rdf] {
    override def makeQueryString(search: String*): String = {
      logger.debug(s"makeQueryString: query $search")
      if (search.size == 0)
        reverseLinks(search(0))
      else
        reverseLinks(
            search(0),
            search(1))
    }
  }

  /** Reverse Links Search; side effect: download URI into TDB in a Future */
  def backlinks(hrefPrefix: String = config.hrefDisplayPrefix,
                request: HTTPrequest): Future[NodeSeq] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val qs = request.getQueries()
    logger.debug(s"backlinks: qs: $qs")
    Future {
      val uri = qs(0)
      retrieveURIBody(
        ops.URI(uri), dataset, request, transactionsInside = true)
    }

    // NOTE queryMaker is passed here implicitly!
    search(
      hrefPrefix,
      request.getLanguage(),
      qs,
      httpRequest = request)
  }

}