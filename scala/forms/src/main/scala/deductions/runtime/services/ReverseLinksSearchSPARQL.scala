package deductions.runtime.services

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.Elem
import org.w3.banana.RDF
import org.w3.banana.SparqlOpsModule
import org.w3.banana.TryW
import org.w3.banana.syntax._
import deductions.runtime.abstract_syntax.InstanceLabelsInference2
import deductions.runtime.dataset.RDFStoreLocalProvider
import deductions.runtime.html.Form2HTML
import org.w3.banana.Transactor

/** Reverse Links Search with simple SPARQL */
trait ReverseLinksSearchSPARQL[Rdf <: RDF, DATASET]
    extends GenericSearchSPARQL[Rdf, DATASET] {

  import ops._
  import sparqlOps._
  import rdfStore.transactorSyntax._
  import rdfStore.sparqlEngineSyntax._

  def makeQueryString(search: String): String =
    s"""
         |SELECT DISTINCT ?thing WHERE {
         |  graph ?g {
         |    ?thing ?p <$search> .
         |  }
         |}""".stripMargin

}