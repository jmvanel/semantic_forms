package deductions.runtime.data_cleaning

import deductions.runtime.sparql_cache.SPARQLHelpers
import deductions.runtime.utils.RDFStoreLocalProvider
import org.w3.banana.{OWLPrefix, RDF}

import scala.language.postfixOps

/** */
trait BlankNodeCleaner[Rdf <: RDF, DATASET]
extends BlankNodeCleanerBatch[Rdf, DATASET]
with BlankNodeCleanerIncremental[Rdf, DATASET]

trait BlankNodeCleanerBase[Rdf <: RDF, DATASET]
    extends RDFStoreLocalProvider[Rdf, DATASET]
    with SPARQLHelpers[Rdf, DATASET] {
  import ops._

//  private val owl = OWLPrefix[Rdf]

//  private lazy val propTypes = List(rdf.Property, owl.ObjectProperty, owl.DatatypeProperty)
//
//  def isProperty(uriTokeep: Rdf#Node): Boolean = {
//    val types = quadQuery(uriTokeep, rdf.typ, ANY).toList
//    logger.debug( s"isProperty( $uriTokeep ) : types $types" )
//    types.exists { typ => propTypes.contains(typ._1.objectt) }
//  }
//
//  private lazy val classTypes = List(rdfs.Class, owl.Class)
//
//  def isClass(uriTokeep: Rdf#Node): Boolean = {
//    val types = quadQuery(uriTokeep, rdf.typ, ANY).toList
//    logger.debug( s"isProperty( $uriTokeep ) : types $types" )
//    types.exists { typ => classTypes.contains(typ._1.objectt) }
//  }

}


trait BlankNodeCleanerIncremental[Rdf <: RDF, DATASET] extends BlankNodeCleanerBase[Rdf, DATASET] {
  import ops._

  /** Search duplicate graph rooted at blank node
   *
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

    def groupByPredicateThenObjectGraphs(graph: Rdf#Graph, uri: Rdf#URI):
    Map[Rdf#URI, Iterable[Rdf#Graph]]= {
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

    logger.debug(s"""\nFor graph URI <graphUri>:
      Search duplicate graph rooted at blank node: input size """ + getTriples(incomingGraph).size)
    val blanksIncomingGraphGroupedByPredicate = groupByPredicateThenObjectGraphs(incomingGraph, graphUri)
    val blanksLocalGraphGroupedByPredicate = {
      val localGraph = rdfStore.getGraph( dataset, graphUri).get // TODO : OrElse(emptyGraph)
      groupByPredicateThenObjectGraphs(localGraph, graphUri)
    }
    logger.debug("Search duplicate graph rooted at blank node: number of predicates, Incoming " + blanksIncomingGraphGroupedByPredicate.size)
    logger.debug("Search duplicate graph rooted at blank node: number of predicates, Local " + blanksLocalGraphGroupedByPredicate.size)

    var localGraphsSizeRemoved = 0
    var localTriplesRemoved = 0
    val keys = blanksLocalGraphGroupedByPredicate.keySet union blanksIncomingGraphGroupedByPredicate.keySet
    for (
      pred <- keys;
      incomingGraphs <- blanksIncomingGraphGroupedByPredicate.get(pred);
      localGraphs <- blanksLocalGraphGroupedByPredicate.get(pred)
    ) {
      logger.debug(
          s"Search duplicate graph rooted at blank node: for predicate <$pred>, number of local graphs Grouped By Predicate " +
          localGraphs.size)
      // and now, complexity O(N^2) !!!
      val removed = scala.collection.mutable.Set[Rdf#Graph]()
      val duplicates = scala.collection.mutable.Set[Rdf#Graph]()
      for (incomingSubGraph <- incomingGraphs) {
        logger.debug("Search duplicate graph rooted at blank node: incoming subgraph size " + getTriples(incomingSubGraph).size)
        for (localGraph <- localGraphs) {
          if (
//              !removed.contains(localGraph) &&
//              !duplicates.contains(incomingSubGraph) &&
            // TODO : also remove if incomingSubGraph contains localGraph
            incomingSubGraph.isIsomorphicWith(localGraph)) {
            // delete local graph
            if (localGraph.size > 0) {
              rdfStore.removeTriples( dataset, graphUri, getTriples(localGraph))
              rdfStore.removeTriples( dataset, graphUri,
                  Seq(
                      Triple(
                      graphUri, pred,
                      getTriples(localGraph).iterator.next().subject)	)
              )
              localGraphsSizeRemoved += 1
              localTriplesRemoved += localGraph.size
            }
            removed += localGraph
            duplicates += incomingSubGraph
            logger.debug(s"Duplicate graph for <$pred> rooted at blank node: deleted:\n\t" + getTriples(localGraph))
          }
        }
      }
    }
    logger.debug(s"Duplicate graph rooted at blank node: number of local graphs removed: $localGraphsSizeRemoved, # of triples removed $localTriplesRemoved\n")
  }
}

trait BlankNodeCleanerBatch[Rdf <: RDF, DATASET]
extends BlankNodeCleanerBase[Rdf, DATASET] {
  import ops._

//  private lazy val rdf = RDFPrefix[Rdf]

  /**
   * Batch program to clean Unreachable Blank Node Sub-Graphs;
   *
   * for all Blank Nodes ?BN, if unreachable,
   * then remove triples
   * ?BN ?P ?O .
   *
   * Unreachable means:
   * there is no triple:
   * ?X ?P ?BN .
   * and the triples
   * ?BN ?P ?O .
   * are such that ?O are all literal,
   */
  def cleanUnreachableBlankNodeSubGraph(): Unit = {
    val triplesAll = find(allNamedGraph, ANY, ANY, ANY)
    logger.debug( s"cleanUnreachableBlankNodeSubGraph: triples size ${triplesAll size}" )
    val names = listNames().toList
    var triplesRemoveCount = 0
    
    for (name <- names) {
      val graphURI = URI(name)
      rdfStore.rw( dataset, {
        val graph = rdfStore.getGraph( dataset, graphURI).get
        logger.debug(s"cleanUnreachableBlankNodeSubGraph: graph <$graphURI> size ${graph.size}")

        val triples = find(graph, ANY, ANY, ANY).toList
        var count = 0
        val blankSubjects = triples.
          map { t =>
            val p = t.predicate
            p != rdf.first &&
            p != rdf.rest
            t.subject
          }.
          filter { s =>
            count = count + 1
            // if (s.isBNode) logger.debug(s"s.isBNode: # $count, $s")
            s.isBNode
          }.
          filter { s =>
            val tt = find(graph, ANY, ANY, s).toList
            // if (!tt.isEmpty) logger.debug("ANY, ANY, s: " + tt)
            find(graph, ANY, ANY, s) isEmpty
          }.
          filter { s =>
            val tt = find(graph, s, ANY, ANY)
            tt.forall { tr => tr.objectt.isLiteral }
          }

       val bss = blankSubjects.toList
       logger.debug(s"=========== blankSubjects size ${blankSubjects size}")
       val mess = bss.map {
          s =>
            if (bss.size > 0) {    
              val triples = find(graph, s, ANY, ANY).toList
              logger.debug("cleanUnreachableBlankNodeSubGraph: TO REMOVE: " + triples)
              triplesRemoveCount = triplesRemoveCount + bss.size
              rdfStore.removeTriples( dataset, graphURI, triples) . get;
              {
              val triples = find(graph, s, ANY, ANY).toList
              logger.debug("cleanUnreachableBlankNodeSubGraph: Verification " + triples)
              s"Remaining size for <$graphURI> ($s): ${triples.size}"
              }
            } else "Nothing to remove."
        }
       logger.debug(mess.toString())
      })
    }
    logger.debug( s"\ntriplesRemoveCount $triplesRemoveCount" )
    ()
  }

  def makeSPARQLremove(s: Rdf#Node, triples: List[Rdf#Triple]): String = {
//    val buf = new StringBuilder
//    for( tr <- triples ) {
//      buf.append( s"""<${(tr.subject)}> <${(tr.predicate)}> "${
//        foldNode(tr.objectt)( _ => "", _ => "", l => fromLiteral(l)._1 ) }".
//        """ )
//    }
  val ret = s"DELETE GRAPH ?G { _:$s ?P ?O .} WHERE { GRAPH ?G { _:$s ?P ?O .} }"
  logger.debug( ret )
  ret
}
  def makeSPARQLselect(s: Rdf#Node): String = {
    s"SELECT * WHERE { GRAPH ?G { _:$s ?P ?O .} }"
  }
  
}
