package deductions.runtime.sparql_cache

import deductions.runtime.sparql_cache.dataset.RDFStoreLocalProvider
import org.w3.banana.RDF

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
trait MirrorManagement[Rdf <: RDF, DATASET] extends RDFStoreLocalProvider[Rdf, DATASET]
  with SPARQLHelpers[Rdf, DATASET]
{

  import ops._

  /** coherent with scripts/populate_with_dbpedia.sh */
  // dbpedia:/home/jmv/data/dbpedia.org/2015-10/labels_en_uris_fr.ttl
  val DBPEDIA_VERSION = "2015-10"
  val DBPEDIA_NAMED_GRAPH = // s"http://dbpedia.org/$DBPEDIA_VERSION"
  s"dbpedia:${System.getProperty("user.home")}/data/dbpedia.org/$DBPEDIA_VERSION/labels_en_uris_fr.ttl"

  lazy val DBPEDIA_NAMED_GRAPH_EXISTS: Boolean = {
    val v = wrapInReadTransaction {
      val tg = rdfStore.getGraph(dataset, URI(DBPEDIA_NAMED_GRAPH))
      val ret = tg.isSuccess && tg.get.size > 0
      println(s"DBPEDIA_NAMED_GRAPH=<$DBPEDIA_NAMED_GRAPH>, DBPEDIA_NAMED_GRAPH_EXISTS: $ret")
      ret
    }
    v.getOrElse(false)
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