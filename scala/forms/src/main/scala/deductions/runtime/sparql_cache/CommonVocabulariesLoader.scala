package deductions.runtime.sparql_cache

import deductions.runtime.jena.RDFStoreLocalJena1Provider
import deductions.runtime.jena.JenaHelpers
import org.w3.banana.Prefix
import org.w3.banana.CertPrefix
import org.w3.banana.DCPrefix
import org.w3.banana.DCTPrefix
import org.w3.banana.FOAFPrefix
import org.w3.banana.RDFModule
import org.w3.banana.RDFOpsModule
import org.w3.banana.RDFPrefix
import org.w3.banana.RDFSPrefix
import org.w3.banana.RDFXMLReaderModule
import org.w3.banana.TurtleReaderModule
import org.w3.banana.WebACLPrefix
import org.w3.banana.OWLPrefix
import org.apache.log4j.Logger
import deductions.runtime.jena.RDFCache
import org.w3.banana.RDF
import deductions.runtime.dataset.RDFStoreLocalProvider
import org.w3.banana.jena.Jena
import com.hp.hpl.jena.query.Dataset

/**
 * @author jmv
 */
object CommonVocabulariesLoader extends RDFCache with App
    with CommonVocabulariesLoaderTrait[Jena, Dataset]
    with RDFStoreLocalJena1Provider
    with JenaHelpers {
  loadCommonVocabularies()
}

trait CommonVocabulariesLoaderTrait[Rdf <: RDF, DATASET]
    extends RDFCacheAlgo[Rdf, DATASET]
    with RDFStoreHelpers[Rdf, DATASET]
    with SitesURLForDownload {

  import ops._
  import rdfStore.transactorSyntax._
  import rdfStore.graphStoreSyntax._

  //  val githubcontent = "https://raw.githubusercontent.com"

  def loadCommonVocabularies() {
    // most used vocab's
    val basicVocabs = List(
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
    import ops._
    val basicVocabsAsURI = basicVocabs map { p => p.apply("") }
    val largerVocabs =
      makeUri("http://usefulinc.com/ns/doap#") ::
        makeUri("http://rdfs.org/sioc/ns#") ::
        makeUri("http://schema.rdfs.org/all.nt") :: // NOTE .ttl is still broken ( asked on https://github.com/mhausenblas/schema-org-rdf/issues/63 )
        makeUri("http://downloads.dbpedia.org/3.9/dbpedia_3.9.owl") ::
        /* geo: , con: */
        makeUri("http://www.w3.org/2003/01/geo/wgs84_pos#") ::
        makeUri("http://www.w3.org/2000/10/swap/pim/contact#") ::
        makeUri(githubcontent + "/assemblee-virtuelle/pair/master/av.owl.ttl") ::
        makeUri("http://purl.org/ontology/cco/cognitivecharacteristics.n3") ::
        Nil

    val vocabs = basicVocabsAsURI ::: largerVocabs

    Logger.getRootLogger().info(vocabs)
    vocabs map {
      voc =>
        try {
          storeURI(voc, dataset)
        } catch {
          case e: Exception => println("Error " + voc + " " + e)
        }
    }
  }

}