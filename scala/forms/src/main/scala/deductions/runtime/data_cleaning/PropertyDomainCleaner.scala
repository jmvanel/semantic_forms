package deductions.runtime.data_cleaning

import org.w3.banana.RDF
import deductions.runtime.dataset.RDFStoreLocalProvider
import deductions.runtime.services.SPARQLHelpers
import org.w3.banana.jena.JenaModule
import deductions.runtime.jena.RDFStoreLocalJenaProvider
import scala.language.postfixOps
import org.w3.banana.RDFSPrefix
import deductions.runtime.utils.RDFHelpers
import deductions.runtime.dataset.RDFOPerationsDB
import deductions.runtime.abstract_syntax.InstanceLabelsInference2
import deductions.runtime.abstract_syntax.PreferredLanguageLiteral
//import java.net.URI
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
  private val rdfs = RDFSPrefix[Rdf]
  private val owl = OWLPrefix[Rdf]
  val newRdfsDomainsGraph = URI("urn:newRdfsDomains/")

  /** replace multiple rdfs:domain's with a single rdfs:domain being a owl:unionOf */
  def replaceMultipleRdfsDomains(property: Rdf#URI) = {
    val triples = find(allNamedGraph, property, rdfs.domain, ANY).toList
    if (triples.size > 1) {
      val binder = PGBinder[Rdf, List[Rdf#Node]]
      val list: List[Rdf#Node] = triples.map { t => t.subject }.toList
      val listPg = binder.toPG(list)
      val graphToAdd = (
        property -- rdfs.domain ->- (
          BNode() -- owl.unionOf ->- listPg.pointer)).graph
      rdfStore.appendToGraph( dataset, newRdfsDomainsGraph, graphToAdd)
      
      // TODO remove triples
      // leverage on quadQuery?
//      rdfStore.removeTriples(dataset, uri, triples)
    }
  }
}
