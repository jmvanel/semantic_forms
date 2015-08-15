package deductions.runtime.services.test

import scala.util.Try
import org.w3.banana.RDF
import org.w3.banana.RDFOps
import org.w3.banana.RDFStore
import org.w3.banana.SparqlEngine
import org.w3.banana.SparqlOps
import org.w3.banana.syntax._
import org.w3.banana.SparqlOpsModule

/** example of boilerplate trait */
trait RDFStoreLocalProvider [Rdf <: RDF, DATASET] {
  implicit val ops: RDFOps[Rdf]
  implicit val rdfStore: RDFStore[Rdf, Try, DATASET]
	implicit val sparqlOps: SparqlOps[Rdf]
  val dataset: DATASET
}

trait ExampleTrait[Rdf <: RDF, DATASET]
    extends RDFStoreLocalProvider[Rdf, DATASET] {
  
  val queryString = s"""
         |CONSTRUCT { ?thing ?p ?o } WHERE {
         |  graph ?g {
         |    ?thing ?p ?o .
         |    FILTER regex( ?o, "search", 'i')
         |  }
         |}""".stripMargin

  import ops._
  import sparqlOps._
  import rdfStore.sparqlEngineSyntax._

  def lookup(search: String) = {
    val graph =
      for {
        query <- parseConstruct(queryString)
        es <- dataset.executeConstruct(query, Map())
      } yield es
    getTriples(graph)  
  }
}