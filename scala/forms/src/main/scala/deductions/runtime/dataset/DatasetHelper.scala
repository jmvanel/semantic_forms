package deductions.runtime.dataset

import org.w3.banana.RDF

trait DatasetHelper[Rdf <: RDF, DATASET]
    extends RDFStoreLocalProvider[Rdf, DATASET] {

	import ops._
  import rdfStore.graphStoreSyntax._
  import rdfStore.transactorSyntax._
  
  /** replace all triples
   *  <subject> <predicate> ?O .
   *  (if any)
   *  with a single one:
   *  <subject> <predicate> <objet> .
   *  
   *  Used for label caching;
   *  can be used for enforcing cardinality 1
   *  */
  def replaceObjects(graphURI: Rdf#URI, subject: Rdf#Node, predicate: Rdf#URI,
      objet: Rdf#Node, ds:DATASET=dataset,
      graph0:Rdf#Graph = emptyGraph
      ): Unit = {
	  val graph = if( graph0 == emptyGraph)
	    rdfStore.getGraph(ds, graphURI).getOrElse(emptyGraph)
	  else
	    graph0
    val objectsToRemove = getObjects(graph, subject, predicate)
    val triplesToRemove = objectsToRemove . map {
      obj => Triple(subject, predicate, obj)
    }
	  ds.removeTriples( graphURI, triplesToRemove )
    ds.appendToGraph( graphURI, makeGraph(List(Triple(subject, predicate, objet))) )
  }
}