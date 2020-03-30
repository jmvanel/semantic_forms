package deductions.runtime.abstract_syntax

import deductions.runtime.sparql_cache.SPARQLHelpers
import deductions.runtime.utils.{RDFHelpers, RDFPrefixes}
import org.w3.banana.RDF

import scala.util.{Failure, Success}

/**
 * Take into account such annotations:
   bioc:Planting form:labelProperty bioc:species.
   (this allows to use ObjectProperty's like bioc:species for computing displayed labels)
 *  */
trait InstanceLabelsFromLabelProperty[Rdf <: RDF, DATASET]
    extends SPARQLHelpers[Rdf, DATASET]
    with RDFHelpers[Rdf]
    with RDFPrefixes[Rdf] {

  import rdfStore.sparqlEngineSyntax._
  import sparqlOps._

  lazy val compiledQuery: Rdf#SelectQuery = {
    val query = s"""
		|${declarePrefix(form)}
    |SELECT ?LABEL_URI
    |WHERE {
    |  GRAPH ?G {
    |    ?CLASS form:labelProperty ?PROP.
    |  }
    |  GRAPH ?GG {
    |    ?thing a ?CLASS .
    |    ?thing ?PROP ?LABEL_URI.
    |} }
    """.stripMargin
    parseSelect(query) match {
      case Success(q) => q
      case Failure(f) =>
        logger.error(s"InstanceLabelsFromLabelProperty: $f")
        parseSelect("SELECT ?Z WHERE{<aa> <bb> <cc> .}").get
    }
  }

 /**
   * inferring possible label from:
   *
   * form:labelProperty in the rdf:type class
   */
  def instanceLabelFromLabelProperty(node: Rdf#Node): Option[Rdf#Node] = {
    ops.foldNode(node)(
      uri => {
        if (uri == nullURI )
          None
        else {
          val bindings: Map[String, Rdf#Node] = Map("?thing" -> uri )
//        	println( s">>>> instanceLabelFromLabelProperty ?thing node $node compiledQuery $compiledQuery" )
        	val solutionsTry = for {
        		es <- dataset.executeSelect(compiledQuery, bindings)
        	} yield es
          makeListofListsFromSolutions(solutionsTry, addHeaderRow = false) match {
            case Success(List(Seq(lab, _*), _*)) =>
              //      	  println( s">>>> instanceLabelFromLabelProperty label $lab" )
              Some(lab)
            case res =>
              //        	    println( s">>>> instanceLabelFromLabelProperty result $res" )
              None
          }
        }
      },
      _ => None,
      _ => None)
  }

}