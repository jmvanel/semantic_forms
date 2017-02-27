package deductions.runtime.sparql_cache

import org.apache.log4j.Logger
import org.w3.banana.CertPrefix
import org.w3.banana.DCPrefix
import org.w3.banana.DCTPrefix
import org.w3.banana.FOAFPrefix
import org.w3.banana.OWLPrefix
import org.w3.banana.Prefix
import org.w3.banana.RDF
import org.w3.banana.RDFPrefix
import org.w3.banana.RDFSPrefix
import org.w3.banana.WebACLPrefix
import org.w3.banana.IANALinkPrefix
import org.w3.banana.LDPPrefix

import deductions.runtime.jena.RDFCache
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.utils.RDFPrefixes
import deductions.runtime.services.DefaultConfiguration
import deductions.runtime.DependenciesForApps

/**
 * @author jmv
 */

/** */
object CommonVocabulariesLoader
    extends  {
      override val config = new DefaultConfiguration {
        override val useTextQuery = false
      }
    }
    with DependenciesForApps
    with CommonVocabulariesLoaderTrait[ImplementationSettings.Rdf, ImplementationSettings.DATASET]
    {
  loadCommonVocabularies()
}

/** TODO common stuff with trait RDFPrefixes[Rdf] ;
 * not trivial;
 * - probably need to distinguish the self-hosted vocabs and the others
 * - distinguish prefixes for data (dbpedia.org/resource) and prefixes for vocabs
 *  */
trait CommonVocabulariesLoaderTrait[Rdf <: RDF, DATASET]
    extends RDFCacheAlgo[Rdf, DATASET]
    with RDFPrefixes[Rdf]
    with SitesURLForDownload {

  import ops._
  import rdfStore.transactorSyntax._
  import rdfStore.graphStoreSyntax._

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
      
  /** larger and less known vocab's */
  val largerVocabs: List[Rdf#URI] =
    URI(githubcontent + "/edumbill/doap/master/schema/doap.rdf") ::
      URI("http://rdfs.org/sioc/ns#") ::
      URI("http://topbraid.org/schema/schema.ttl") ::
      /* NOTES
       * schema.rdfs.org is down on 25 april 2016
       * .ttl is still broken ?
       * ( asked on https://github.com/mhausenblas/schema-org-rdf/issues/63 )
       * 
       * TODO the best is to use the RDFa official version at
       * https://github.com/schemaorg/schemaorg/blob/sdo-callisto/data/schema.rdfa */
//    	URI("http://schema.rdfs.org/all.nt") ::
      /* see also scripts/download-dbpedia.sh in Semantic_forms */
      URI("http://downloads.dbpedia.org/2015-10/dbpedia_2015-10.nt") ::
      /* geo: , con: */
      URI("http://www.w3.org/2003/01/geo/wgs84_pos#") ::
      URI("http://www.w3.org/2000/10/swap/pim/contact#") ::
      // URI(githubcontent + "/assemblee-virtuelle/pair/master/PAIR_1.0.owl.ttl" ) ::
      URI("http://purl.org/ontology/cco/cognitivecharacteristics.n3") ::
      prefixesMap("skos") ::
      prefixesMap("vcard") ::
      URI(githubcontent + "/jmvanel/semantic_forms/master/scala/forms/form_specs/additions_to_vocabs.ttl" ) ::
      prefixesMap("tm") ::
      prefixesMap("bioc") ::
      prefixesMap("doas") ::
      Nil
      // "http://purl.org/ontology/mo/"
   
  /**
   * TRANSACTIONAL
   *  basicVocabs are not supposed to change often ..
   */
  def resetCommonVocabularies() {
    val r = rdfStore.rw( dataset, {
      largerVocabs map {
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
    import ops._
    Logger.getRootLogger().info(vocabularies)
    vocabularies map {
      voc =>
        try {
          readStoreUriInNamedGraph(voc)
            println("JVM Total memory (bytes): " +
              Runtime.getRuntime().totalMemory());
            System.gc()
            println("JVM Free memory after gc(): " +
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
              (if (maxMemory == Long.MaxValue) "no limit" else maxMemory));

            /* Total memory currently in use by the JVM */
            System.err.println("JVM Total memory (bytes): " +
              Runtime.getRuntime().totalMemory());
            System.gc()
            System.err.println("JVM Free memory after gc(): " +
              Runtime.getRuntime().freeMemory())        }
    }
  }

}
