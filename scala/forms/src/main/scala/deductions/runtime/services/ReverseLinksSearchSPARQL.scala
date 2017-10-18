package deductions.runtime.services

import org.w3.banana.RDF

import scala.concurrent.Future
import scala.xml.NodeSeq
import deductions.runtime.core.HTTPrequest

/** Reverse Links Search with simple SPARQL */
trait ReverseLinksSearchSPARQL[Rdf <: RDF, DATASET]
  extends ParameterizedSPARQL[Rdf, DATASET]
  with StringSearchSPARQLBase[Rdf]
  with NavigationSPARQLBase[Rdf] {

  private implicit val queryMaker = new SPARQLQueryMaker[Rdf] {
    override def makeQueryString(search: String*): String = {
 //         println( s"query $q")
         reverseLinks(search(0))
    }
  }

  def backlinks(uri: String, hrefPrefix: String = config.hrefDisplayPrefix,
      request:HTTPrequest): Future[NodeSeq] =
    search(hrefPrefix,
      request.getLanguage(),
      Seq(uri))

}