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
import org.w3.banana.RDFStore
import org.w3.banana.GraphStore
import org.w3.banana.OWLPrefix
import deductions.runtime.jena.RDFStoreObject
import org.apache.log4j.Logger
import org.w3.banana.Prefix // Builder
import org.w3.banana.SparqlUpdate
import org.w3.banana.RDFOps
import org.w3.banana.RDFDSL

/**
 * Populate RDF Cache with commonly used vocabularies;
 *  should be done just once;
 *
 *  same as loading
 *  http://svn.code.sf.net/p/eulergui/code/trunk/eulergui/examples/defaultVocabularies.n3p.n3
 *  but without any dependency to EulerGUI.
 */
object PopulateRDFCache extends RDFCache
//with SparqlUpdate
with App {

  loadCommonVocabularies
  loadCommonFormSpecifications()

  def loadCommonVocabularies() {
    //  val uri: Rdf#URI = RDFSPrefix[Rdf].apply("")
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
      CertPrefix[Rdf], OWLPrefix[Rdf])
    import ops._
    val vocabs1 = (vocabs0 map { p => p.apply("") })
    val vocabs2 =
      makeUri("http://usefulinc.com/ns/doap#") ::
        makeUri("http://rdfs.org/sioc/ns#") ::
        //     makeUri("http://schema.rdfs.org/all.ttl") :: // is still broken ( asked to Richard & Michael )
        makeUri("http://downloads.dbpedia.org/3.9/dbpedia_3.9.owl") :: // OK with Jena 2.11.1 !!!
        Nil

    val vocabs = vocabs1 ::: vocabs2
    /* geo: , con: , <foaf_fr.n3>  TODO */

    Logger.getRootLogger().info(vocabs)
//    vocabs1 map { storeURI(_, store) }
    vocabs1 map { storeURI( _, dataset ) }
  }
  
  /** load CommonForm Specifications from a well know place */
  def loadCommonFormSpecifications()  {
    val all_form_specs = "https://raw.githubusercontent.com/jmvanel/semantic_forms/master/scala/forms/form_specs/specs.ttl"    
    val from = new java.net.URL(all_form_specs).openStream()
    val form_specs_graph: Rdf#Graph = turtleReader.read(from, base = all_form_specs) getOrElse sys.error(s"couldn't read $all_form_specs")
    import deductions.runtime.abstract_syntax.FormSyntaxFactory._
    val formPrefix = Prefix("form", formVocabPrefix ) 
    
//    val store =  RDFStoreObject.store

    /* Retrieving :
     * foaf: form:ontologyHasFormSpecification <foaf.form.ttl> . */
    val triples : Iterator[Rdf#Triple] 
       = ops.find( form_specs_graph, ops.ANY, formPrefix("ontologyHasFormSpecification"), ops.ANY)
    val objects = for( triple <- triples) yield {
      triple.getObject
    } 
    for( obj <- objects ) {
          val from = new java.net.URL(obj.toString()).openStream()
          val form_spec_graph: Rdf#Graph = turtleReader.read( from, base = obj.toString() ) getOrElse sys.error(s"couldn't read ${obj.toString()}")
//          store.appendToGraph( Ops.makeUri("form_specs"), form_spec_graph )
          rdfStore.appendToGraph( dataset, ops.makeUri("form_specs"), form_spec_graph )
          println("Added form_spec " + obj)
    }
  }
}