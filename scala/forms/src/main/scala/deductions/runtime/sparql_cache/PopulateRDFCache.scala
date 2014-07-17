package deductions.runtime.sparql_cache

import org.apache.jena.riot.RDFDataMgr
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
import org.w3.banana.jena.JenaModule
import org.w3.banana.jena.JenaStore
import com.hp.hpl.jena.tdb.TDBFactory
import org.w3.banana.RDFStore
import org.w3.banana.GraphStore
import org.w3.banana.OWLPrefix
import deductions.runtime.jena.RDFStoreObject

/** Populate RDF Cache with commonly used vocabularies;
 *  should be done just once;
 *  
 *  same as loading
 *  http://svn.code.sf.net/p/eulergui/code/trunk/eulergui/examples/defaultVocabularies.n3p.n3
 *  but without any dependency to EulerGUI.
 *   */
object PopulateRDFCache extends RDFCacheJena with App {
  
  loadCommonVocabularies
  
  def loadCommonVocabularies() {
  val store = RDFStoreObject.store
  val uri: Rdf#URI = RDFSPrefix[Rdf].apply("")
  // loop on most used vocab's
  val vocabs0 = List(
    RDFPrefix[Rdf],
    RDFSPrefix[Rdf],
    DCPrefix[Rdf],
    DCTPrefix[Rdf],
    FOAFPrefix[Rdf],
//    LDPPrefix[Rdf],
//    IANALinkPrefix[Rdf],
    WebACLPrefix[Rdf],
    CertPrefix[Rdf]
    ,OWLPrefix[Rdf]
    )
    import Ops._
    val vocabs1 = ( vocabs0 map { p => p.apply("")} )
    val vocabs2 = 
     makeUri("http://usefulinc.com/ns/doap#") :: 
     makeUri("http://rdfs.org/sioc/ns#") ::
//     makeUri("http://schema.rdfs.org/all.ttl") :: // is still broken ( asked to Richard & Michael )
     makeUri("http://downloads.dbpedia.org/3.9/dbpedia_3.9.owl") :: // OK with Jena 2.11.1 !!!
    	 Nil
     
     val vocabs = vocabs1 ::: vocabs2
  /* geo: , con: , <foaf_fr.n3>  TODO */

     vocabs2 map { storeURI( _, store) }
  }
  }