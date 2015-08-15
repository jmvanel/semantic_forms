package deductions.runtime.services.test

import scala.util.Try
import org.w3.banana.RDF
import org.w3.banana.RDFOps
import org.w3.banana.RDFStore
import org.w3.banana.SparqlEngine
import org.w3.banana.SparqlOps
import org.w3.banana.syntax._
import org.w3.banana.SparqlOpsModule
import org.w3.banana.RDFOpsModule

/** example of boilerplate trait */
trait RDFStoreLocalProvider[Rdf <: RDF, DATASET] // extends RDFOpsModule
{
  implicit val ops: RDFOps[Rdf]
  implicit val rdfStore: RDFStore[Rdf, Try, DATASET]
  implicit val sparqlOps: SparqlOps[Rdf]
  val dataset: DATASET
  def allNamedGraphs: Rdf#Graph
}

trait InstanceLabelsInference[Rdf <: RDF] {
  def instanceLabel(uri: Rdf#Node, graph: Rdf#Graph, lang: String = ""): String = "bla"
}

trait ExampleTrait[Rdf <: RDF, DATASET]
    extends RDFStoreLocalProvider[Rdf, DATASET]
    with InstanceLabelsInference[Rdf] {

  import ops._
  import sparqlOps._

  val queryString = s"""
         |CONSTRUCT { ?thing ?p ?o } WHERE {
         |  graph ?g {
         |    ?thing ?p ?o .
         |    FILTER regex( ?o, "search", 'i')
         |  }
         |}""".stripMargin

  import rdfStore.sparqlEngineSyntax._

  def lookup(search: String) = {
    for {
      query <- parseConstruct(queryString)
      es <- dataset.executeConstruct(query)
    } yield getTriples(es)
  }

  def display(uri: Rdf#URI) {
    instanceLabel(uri, allNamedGraphs, "")
  }

}