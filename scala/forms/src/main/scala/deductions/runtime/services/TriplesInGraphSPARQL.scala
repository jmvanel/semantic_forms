package deductions.runtime.services

import org.w3.banana.RDF

/** Show named graphs */
trait TriplesInGraphSPARQL[Rdf <: RDF, DATASET]
    extends ParameterizedSPARQL[Rdf, DATASET] {

  private def where(graphURI: String*): String = s"""
        |WHERE {
        |  graph <${graphURI(0)}> {
        |    ?thing ?p ?o .
        |  }
        |}""".stripMargin

  private implicit val searchStringQueryMaker =
    new SPARQLQueryMaker[Rdf] {
    override def makeQueryString(graphURI: String*): String =
      s"""
         |SELECT DISTINCT ?thing ?p ?o
         |  ${where(graphURI:_*)}
         |LIMIT 500""".stripMargin
      /* LIMIT 500 because of computed labels Graph urn:/semforms/labelsGraphUri/ 
       * in the case of a large database , e.g. dbPedia mirror */

    override def variables = Seq("thing", "p", "o")

    def constructQuery(graphURI: String*): String =
      s"""|construct {?thing ?p ?o}
          |  ${where(graphURI:_*)}""".stripMargin
//    override def columnsForURI( node: Rdf#Node, label: String): NodeSeq =
//      Text("test")    
  }

  def showTriplesInGraph(graphURI: String, lang: String = "") //  : Future[Elem]
  = {
//		  println(s"showTriplesInGraph: hrefDisplayPrefix ${config.hrefDisplayPrefix}")
    <p>
      <p> Triples in Graph &lt;{ graphURI }> 
        <a href={"/sparql?query=" + searchStringQueryMaker.constructQuery(graphURI)} class="sf-local-rdf-link">Data export: download Triples</a>
      </p>
      { search2(graphURI, config.hrefDisplayPrefix, lang) }
    </p>
  }

}