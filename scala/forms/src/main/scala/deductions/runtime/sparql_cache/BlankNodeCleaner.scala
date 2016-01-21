package deductions.runtime.sparql_cache

import org.w3.banana.RDF

import deductions.runtime.dataset.RDFStoreLocalProvider


trait BlankNodeCleaner [Rdf <: RDF, DATASET]
extends RDFStoreLocalProvider[Rdf, DATASET] {
  
  import ops._
  import rdfStore.graphStoreSyntax._
  
  def main(args: Array[String]): Unit = {
    // TODO
  }

    /**
   * cf post on Jena list:
   * http://mail-archives.apache.org/mod_mbox/jena-users/201507.mbox/%3C55943D78.30307@apache.org%3E
   *
   * When saving <uri1> ,
   * must delete existing triples
   * <uri1> ?P1 _:bn11_old . _:bn11_old . ?P2 ?O2 .
   * when _:bn11_old has a display label equal to the one of incoming blank node _:bn11 such that:
   * <uri1> ?P1 _:bn11 .
   */
  def manageBlankNodesReload(incomingGraph: Rdf#Graph, graphUri: Rdf#URI,
      dataset: DATASET) = {

    def groupByPredicateThenObjectGraphs(graph: Rdf#Graph, uri: Rdf#URI) = {
      val triples = find(graph, uri, ANY, ANY)
      // these are the triples  <uri1> ?P1 _:bn11_old .
      val blankTriples = triples.filter { t => t.objectt.isBNode }.toIterable

      blankTriples.groupBy { t => t.predicate }.map {
        case (pred, triplesVar) => (pred, triplesVar.map {
          // here we get the triples  _:bn11_old . ?P2 ?O2 .
          tr =>
            val triplesFromBlank = find(graph, tr.objectt, ANY, ANY).toIterable
            makeGraph(triplesFromBlank)
        })
      }
    }

    println("Search duplicate graph rooted at blank node: input size " + getTriples(incomingGraph).size)
    val blanksIncomingGraphGroupedByPredicate = groupByPredicateThenObjectGraphs(incomingGraph, graphUri)
    val blanksLocalGraphGroupedByPredicate = {
      val localGraph = dataset.getGraph(graphUri).get // TODO : OrElse(emptyGraph)
      groupByPredicateThenObjectGraphs(localGraph, graphUri)
    }
    println("Search duplicate graph rooted at blank node: number of predicates Incoming " + blanksIncomingGraphGroupedByPredicate.size)
    println("Search duplicate graph rooted at blank node: number of predicates Local " + blanksLocalGraphGroupedByPredicate.size)

    val keys = blanksLocalGraphGroupedByPredicate.keySet union blanksIncomingGraphGroupedByPredicate.keySet
    for (pred <- keys) {
      val incomingGraphs = blanksIncomingGraphGroupedByPredicate.get(pred).get
      val localGraphs = blanksLocalGraphGroupedByPredicate.get(pred).get
      println(s"Search duplicate graph rooted at blank node: for predicate $pred Local, number of graphs " + localGraphs.size)
      // and now, complexity O(N^2) !!!
      val removed = scala.collection.mutable.Set[Rdf#Graph]()
      val duplicates = scala.collection.mutable.Set[Rdf#Graph]()
      for (incomingSubGraph <- incomingGraphs) {
        println("Search duplicate graph rooted at blank node: subgraph size " + getTriples(incomingSubGraph).size)
        for (localGraph <- localGraphs) {
          if (!removed.contains(localGraph) &&
            !duplicates.contains(incomingSubGraph) &&
            incomingSubGraph.isIsomorphicWith(localGraph)) {
            // delete local graph
            dataset.removeTriples(graphUri, getTriples(localGraph))
            if (localGraph.size > 0) {
              dataset.removeTriples(graphUri, Seq(Triple(graphUri, pred,
                getTriples(localGraph).iterator.next().subject)))
            }
            removed += localGraph
            duplicates += incomingSubGraph
            println("Duplicate graph rooted at blank node: deleted:\n" + getTriples(incomingSubGraph))
          }
        }
      }
    }
  }
  
}