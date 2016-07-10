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
import org.w3.banana.jena.Jena
import org.w3.banana.jena.JenaModule
import deductions.runtime.jena.RDFCache
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import deductions.runtime.jena.ImplementationSettings

/**
 * @author jmv
 */

/** TODO put in package jena */
object CommonVocabulariesLoader extends JenaModule
    with RDFCache with App
    with CommonVocabulariesLoaderTrait[Jena, ImplementationSettings.DATASET]
    with RDFStoreLocalJena1Provider //    with JenaHelpers
    {
  loadCommonVocabularies()
}

trait CommonVocabulariesLoaderTrait[Rdf <: RDF, DATASET]
    extends RDFCacheAlgo[Rdf, DATASET]
    //    with RDFStoreHelpers[Rdf, DATASET]
    with SitesURLForDownload {

  import ops._
  import rdfStore.transactorSyntax._
  import rdfStore.graphStoreSyntax._

  /** most used vocab's */
  val basicVocabs: List[Prefix[Rdf]] = List(
    RDFPrefix[Rdf],
    RDFSPrefix[Rdf],
    DCPrefix[Rdf],
    DCTPrefix[Rdf],
    FOAFPrefix[Rdf],
    //    LDPPrefix[Rdf],
    //    IANALinkPrefix[Rdf],
    WebACLPrefix[Rdf],
    CertPrefix[Rdf],
    OWLPrefix[Rdf])

    import scala.language.postfixOps
    
  /** larger and less known vocab's */
  val largerVocabs: List[Rdf#URI] =
    // makeUri("http://usefulinc.com/ns/doap#") ::
    URI("https://raw.githubusercontent.com/edumbill/doap/master/schema/doap.rdf") ::
      makeUri("http://rdfs.org/sioc/ns#") ::   
      makeUri("http://topbraid.org/schema/schema.ttl") ::
      // makeUri("http://schema.rdfs.org/all.nt") ::
      /* NOTES
       * schema.rdfs.org is down on 25 april 2016
       * .ttl is still broken ?
       * ( asked on https://github.com/mhausenblas/schema-org-rdf/issues/63 ) */
      makeUri("http://downloads.dbpedia.org/3.9/dbpedia_3.9.owl") ::
      /* geo: , con: */
      makeUri("http://www.w3.org/2003/01/geo/wgs84_pos#") ::
      makeUri("http://www.w3.org/2000/10/swap/pim/contact#") ::
      makeUri(githubcontent + "/assemblee-virtuelle/pair/master/av.owl.ttl") ::
      makeUri("http://purl.org/ontology/cco/cognitivecharacteristics.n3") ::
      makeUri("http://www.w3.org/2004/02/skos/core#") ::
      // "http://purl.org/ontology/mo/"
      Nil

  /**
   * TRANSACTIONAL
   *  basicVocabs are not supposed to change often ..
   */
  def resetCommonVocabularies() {
    val r = dataset.rw({
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

  /** TRANSACTIONAL */
  def loadCommonVocabularies() {
    import ops._
    val basicVocabsAsURI = basicVocabs map { p => p.apply("") }
    val vocabs = basicVocabsAsURI ::: largerVocabs
    Logger.getRootLogger().info(vocabs)
    vocabs map {
      voc =>
        try {
          storeUriInNamedGraph(voc)
            println("JVM Total memory (bytes): " +
              Runtime.getRuntime().totalMemory());
            System.gc()
            println("JVM Free memory after gc(): " +
              Runtime.getRuntime().freeMemory())
        } catch {
          case e: Exception =>
            System.err.println("Error in loadCommonVocabularies: " +
                voc + " " + e)
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
