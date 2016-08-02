package deductions.runtime.sparql_cache

import org.w3.banana.RDF

import deductions.runtime.dataset.RDFStoreLocalProvider

/**
 * Within the SPARQL TDB cache,
 * there is a need to mirror big datasets like dbPedia. This allows to query and reason on the whole dbPedia semantic network,
 * plus the labels for dbPedia resources will always be accurate.
 *
 * Originally the SPARQL TDB cache downloads and caches any dbPedia resource URL,
 * even if the data is already there locally in a global named graph for dbPedia.
 *
 * So with this feature "MirrorManagement", the cache, when retrieving an URI <u1> ,
 * first checks if the prefix for <u1> has an associated global named graph, and then just return the triples
 *
 * <u1> ?P ?O .
 */
trait MirrorManagement[Rdf <: RDF, DATASET] extends RDFStoreLocalProvider[Rdf, DATASET] {

  import ops._

  /** coherent with scripts/populate_with_dbpedia.sh */
  val DBPEDIA_VERSION = "2015-04"
  val DBPEDIA_NAMED_GRAPH = s"http://dbpedia.org/$DBPEDIA_VERSION"
  lazy val DBPEDIA_NAMED_GRAPH_EXISTS: Boolean = {
    val tg = rdfStore.getGraph(dataset, URI(DBPEDIA_NAMED_GRAPH))
    tg.isSuccess && tg.get.size > 0
  }

  /**
   * get Mirror URI, that is the named graph URI for the mirror,
   *  or "" if none
   * (used for dbPedia)
   */
  def getMirrorURI(uri: Rdf#URI): String = {
    if (fromUri(uri).startsWith("http://dbpedia.org/resource/") &&
      DBPEDIA_NAMED_GRAPH_EXISTS)
      DBPEDIA_NAMED_GRAPH
    else
      ""
  }
}