package deductions.runtime.services

import org.w3.banana.RDF
import scala.xml.Elem

/** Show named graphs */
trait TriplesInGraphSPARQL[Rdf <: RDF, DATASET]
    extends ParameterizedSPARQL[Rdf, DATASET] {

  private implicit val searchStringQueryMaker = new SPARQLQueryMaker[Rdf] {

    override def variables = Seq("thing", "p", "o")

    override def makeQueryString(graphURI: String*): String =
      s"""|construct {?thing ?p ?o}
          |  ${where(graphURI:_*)}""".stripMargin
//    override def columnsForURI( node: Rdf#Node, label: String): NodeSeq =
//      Text("test")    

      private def where(graphURI: String*): String = s"""
        |WHERE {
        |  graph <${graphURI(0)}> {
        |    ?thing ?p ?o .
        |  }
        |}""".stripMargin
  }

  def showTriplesInGraph(graphURI: String, lang: String = "") : Elem = {
    <p>
      <p> Triples in Graph &lt;{ graphURI }> 
        <a href={"/sparql?query=" + searchStringQueryMaker.makeQueryString(graphURI)}
           class="sf-local-rdf-link">Data export: download Triples</a>
      </p>
      { search2(graphURI, config.hrefDisplayPrefix, lang) }
    </p>
  }

}