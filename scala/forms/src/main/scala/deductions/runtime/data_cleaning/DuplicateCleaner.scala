package deductions.runtime.data_cleaning

import org.w3.banana.RDF
import deductions.runtime.dataset.RDFStoreLocalProvider
import deductions.runtime.services.SPARQLHelpers
import org.w3.banana.jena.JenaModule
import deductions.runtime.jena.RDFStoreLocalJenaProvider
import scala.language.postfixOps
import org.w3.banana.RDFPrefix
import deductions.runtime.utils.RDFHelpers
import deductions.runtime.dataset.RDFOPerationsDB
import deductions.runtime.sparql_cache.BlankNodeCleanerBase
import deductions.runtime.abstract_syntax.InstanceLabelsInference2
import deductions.runtime.abstract_syntax.PreferredLanguageLiteral

/** merge FOAF duplicates #41  */
trait DuplicateCleaner[Rdf <: RDF, DATASET]
extends BlankNodeCleanerBase[Rdf, DATASET]
with InstanceLabelsInference2[Rdf]
with PreferredLanguageLiteral[Rdf] {
  import ops._
  import rdfStore.graphStoreSyntax._
  import rdfStore.transactorSyntax._

//  val rdf = RDFPrefix[Rdf]
  
  /** */
  def clean(uri: Rdf#URI, classURI: Rdf#URI, lang: String = "") = {
    
//      instanceLabel(node: Rdf#Node, graph: Rdf#Graph, lang: String = ""): String = {
    val label = instanceLabel( uri, allNamedGraph, lang)

    val triples = find( allNamedGraph, ANY, rdf.typ, classURI)
    val duplicateTriples = triples.filter {
      t => t.subject != uri &&
      instanceLabel( t.subject, allNamedGraph, lang) == label }.toIterable

    // TODO remove quads for the duplicate
  }
}