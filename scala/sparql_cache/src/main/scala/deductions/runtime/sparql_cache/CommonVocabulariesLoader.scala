package deductions.runtime.sparql_cache

import deductions.runtime.utils.RDFPrefixes
// import org.apache.log4j.Logger
import org.w3.banana.{CertPrefix, DCPrefix, DCTPrefix, FOAFPrefix, LDPPrefix, OWLPrefix, Prefix, RDF, RDFPrefix, RDFSPrefix, WebACLPrefix}

import scalaz._
import Scalaz._

/** TODO common stuff with trait RDFPrefixes[Rdf] ;
 * not trivial;
 * - probably need to distinguish the self-hosted vocabs and the others
 * - distinguish prefixes for data (dbpedia.org/resource) and prefixes for vocabs
 *  */
trait CommonVocabulariesLoader[Rdf <: RDF, DATASET]
    extends RDFCacheAlgo[Rdf, DATASET]
    with RDFPrefixes[Rdf]
    with SitesURLForDownload {

  import ops._

  /** most used vocab's; they are "self-hosted" */
  val basicVocabs: List[Prefix[Rdf]] = List(
    RDFPrefix[Rdf],
    RDFSPrefix[Rdf],
    DCPrefix[Rdf],
    DCTPrefix[Rdf],
    FOAFPrefix[Rdf],
    LDPPrefix[Rdf],
//    IANALinkPrefix[Rdf],
    WebACLPrefix[Rdf],
    CertPrefix[Rdf],
    OWLPrefix[Rdf])

    import scala.language.postfixOps
      
  /** larger and less known vocab's, they are NOT necessarily "self-hosted" */
  val largerVocabs: List[Rdf#URI] =
//    URI(githubcontent + "/edumbill/doap/master/schema/doap.rdf") ::
      URI("https://github.com/ewilderj/doap/raw/master/schema/doap.rdf") ::
      URI("http://rdfs.org/sioc/ns#") ::
      URI(orgVocab.prefixIri) ::
      URI("http://schema.org/version/3.4/schema.ttl") ::
      /* see also scripts/download-dbpedia.sh in Semantic_forms */
      URI("http://downloads.dbpedia.org/2016-10/dbpedia_2016-10.nt") ::
      URI("http://www.w3.org/2003/01/geo/wgs84_pos#") ::
      /* con: */
      URI("http://www.w3.org/2000/10/swap/pim/contact#") ::
      // URI(githubcontent + "/assemblee-virtuelle/pair/master/PAIR_1.0.owl.ttl" ) ::
      URI("http://purl.org/ontology/cco/cognitivecharacteristics.n3") ::
      prefixesMap("skos") ::
      prefixesMap("vcard") ::
      URI(githubcontent + "/jmvanel/semantic_forms/master/scala/forms/form_specs/additions_to_vocabs.ttl" ) ::
      form("") ::
      prefixesMap("tm") ::
      prefixesMap("bioc") ::
      prefixesMap("seeds") ::
      prefixesMap("doas") ::
      prefixesMap("nature") ::
      URI(geo.prefixIri) ::
      URI(geoloc.prefixIri) ::
      URI(vehman.prefixIri) ::
      URI(event.prefixIri) ::
      URI("http://vocab.deri.ie/void.ttl") ::
      prefixesMap("dcat") ::
      Nil
      // "http://purl.org/ontology/mo/"
   
  /**
   * TRANSACTIONAL
   *  #basicVocabs (FOAF etc) are not supposed to change often, so they are not removed ..
   */
  def resetCommonVocabularies(except:List[Rdf#URI] = List()) {
    val r = rdfStore.rw( dataset, {
      (largerVocabs diff except) map {
        voc =>
          try {
            rdfStore.removeGraph( dataset, voc)
          } catch {
            case e: Exception => println("Error in resetCommonVocabularies " + voc + " " + e)
          }
      }
    })
  }

  val vocabularies = {
    val basicVocabsAsURI = basicVocabs map { _.apply("") }
    basicVocabsAsURI ::: largerVocabs
  }

  /** TRANSACTIONAL */
  def loadCommonVocabularies() {
    // Logger.getRootLogger().info(vocabularies)
    vocabularies map {
      voc =>
        try {
          println(s"load Common Vocabulary <$voc>")
          readStoreUriInNamedGraph(voc)
            print("JVM Total memory (bytes): " +
              Runtime.getRuntime().totalMemory());
            System.gc()
            println(" -- JVM Free memory after gc(): " +
              Runtime.getRuntime().freeMemory())
        } catch {
          case e: Exception =>
            System.err.println(s"""Error in loadCommonVocabularies:
                vocabulary <$voc> Exception: $e
                ${e.printStackTrace()}""")

            /* Total number of processors or cores available to the JVM */
            System.err.println("Available processors (cores): " +
              Runtime.getRuntime().availableProcessors());

            /* Total amount of free memory available to the JVM */
            System.err.println("JVM Free memory (bytes): " +
              Runtime.getRuntime().freeMemory())

            /* This will return Long.MAX_VALUE if there is no preset limit */
            val maxMemory = Runtime.getRuntime().maxMemory();
            /* Maximum amount of memory the JVM will attempt to use */
            System.err.println("JVM Maximum memory (bytes): " +
              (if (maxMemory === Long.MaxValue) "no limit" else maxMemory));

            /* Total memory currently in use by the JVM */
            System.err.println("JVM Total memory (bytes): " +
              Runtime.getRuntime().totalMemory());
            System.gc()
            System.err.println("JVM Free memory after gc(): " +
              Runtime.getRuntime().freeMemory())        }
    }
  }

    def loadCommonVocabulariesTest() {
    	readStoreUriInNamedGraph( URI(githubcontent + "/jmvanel/semantic_forms/master/scala/forms/form_specs/additions_to_vocabs.ttl") )
    }

}
