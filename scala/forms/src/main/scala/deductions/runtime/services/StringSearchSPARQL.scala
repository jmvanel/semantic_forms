package deductions.runtime.services

import org.w3.banana.RDF
import scala.concurrent.Future
import scala.xml.Elem

/** String Search with simple SPARQL */
trait StringSearchSPARQL[Rdf <: RDF, DATASET]
    extends ParameterizedSPARQL[Rdf, DATASET] {

  private implicit val searchStringQueryMaker = new SPARQLQueryMaker {
    override def makeQueryString(search: String): String =
      s"""
         |SELECT DISTINCT ?thing WHERE {
         |  graph ?g {
         |    ?thing ?p ?o .
         |    FILTER regex( ?o, "$search", 'i')
         |  }
         |}""".stripMargin
  }

  private val searchStringQueryMaker2 = new SPARQLQueryMaker {
    override def makeQueryString(search: String): String =
      s"""
         |SELECT DISTINCT ?thing WHERE {
         |  {
         |  graph ?g {
         |    ?thing ?p ?o .
         |    FILTER regex( ?o, "$search", 'i')
         |  }
         |  } UNION {
         |  graph ?g0 {
         |    ?thing ?p ?o .
         |  }
         |  graph ?g1 {
         |   ?o ?p2 ?o2 .
         |    FILTER regex( ?o2, "$search", 'i')
         |  }
         |  }
         |}""".stripMargin
  }

  def searchString(searchString: String, hrefPrefix: String = "",
      lang: String = ""): Future[Elem] =
    search(searchString, hrefPrefix, lang)

}