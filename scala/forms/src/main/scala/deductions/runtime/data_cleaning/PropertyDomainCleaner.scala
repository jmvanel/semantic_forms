package deductions.runtime.data_cleaning

import org.w3.banana.RDF
import deductions.runtime.dataset.RDFStoreLocalProvider
import deductions.runtime.services.SPARQLHelpers
import org.w3.banana.jena.JenaModule
import deductions.runtime.jena.RDFStoreLocalJenaProvider
import scala.language.postfixOps

import deductions.runtime.utils.RDFHelpers
import deductions.runtime.dataset.RDFOPerationsDB
import deductions.runtime.abstract_syntax.InstanceLabelsInference2
import deductions.runtime.abstract_syntax.PreferredLanguageLiteral

import deductions.runtime.services.URIManagement
import org.w3.banana.binder.PGBinder
import org.w3.banana.OWLPrefix

/**
 * After merging RDF Property duplicates (in trait DuplicateCleaner),
 * replace multiple rdfs:domain's with a single rdfs:domain being a owl:unionOf
 */
trait PropertyDomainCleaner[Rdf <: RDF, DATASET]
    extends BlankNodeCleanerBase[Rdf, DATASET]
    with InstanceLabelsInference2[Rdf]
    with PreferredLanguageLiteral[Rdf]
    with URIManagement {

  import ops._
  import rdfStore.graphStoreSyntax._
  import rdfStore.transactorSyntax._

  private val owl = OWLPrefix[Rdf]
  val newRdfsDomainsGraph = URI("urn:newRdfsDomains/")

  /**
   * replace multiple rdfs:domain's with a single rdfs:domain being a owl:unionOf ;
   * includes transaction
   */
  def processMultipleRdfsDomains(uriTokeep: Rdf#URI, duplicateURIs: Seq[Rdf#URI]) = {
    println(s"processMultipleRdfsDomains: uriTokeep <$uriTokeep>, duplicateURIs ${duplicateURIs.mkString(", ")}")
    val transaction = rdfStore.rw(dataset, {
      if (isProperty(uriTokeep)) replaceMultipleRdfsDomains(uriTokeep)
    })
    println(s"replaceMultipleRdfsDomains: transaction $transaction dataset $dataset")
  }

  /** replace multiple rdfs:domain's with a single rdfs:domain being a owl:unionOf */
  def replaceMultipleRdfsDomains(property: Rdf#URI) = {
    val triples = find(allNamedGraph, property, rdfs.domain, ANY).toList
    println(s"replaceMultipleRdfsDomains: triples $triples")
    if (triples.size > 1) {
      val binder = PGBinder[Rdf, List[Rdf#Node]]
      val list: List[Rdf#Node] = triples.map { t => t.objectt }.toList
      val listPg = binder.toPG(list)
      val graphToAdd = (
        property -- rdfs.domain ->- (BNode()
          -- rdf.typ ->- owl.Class
          -- owl.unionOf ->- listPg.pointer)).graph union listPg.graph
      println(s"replaceMultipleRdfsDomains: graphToAdd $graphToAdd")
      rdfStore.appendToGraph(dataset, newRdfsDomainsGraph, graphToAdd)

      // remove original triples <property> rdfs:domain ?CLASS .
      for (triple <- triples)
        removeFromQuadQuery(triple.subject, triple.predicate, triple.objectt)
    }
  }
}
