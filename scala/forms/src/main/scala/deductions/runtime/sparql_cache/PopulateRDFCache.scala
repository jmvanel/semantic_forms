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
import org.w3.banana.Prefix
import org.w3.banana.SparqlUpdate
import org.w3.banana.RDFOps
import org.w3.banana.RDFDSL
import scala.util.Failure
import scala.util.Try
import scala.util.Success
import org.w3.banana.LocalNameException

/**
 * Populate RDF Cache with commonly used vocabularies;
 *  should be done just once;
 *
 *  same as loading
 *  http://svn.code.sf.net/p/eulergui/code/trunk/eulergui/examples/defaultVocabularies.n3p.n3
 *  but without any dependency to EulerGUI.
 *
 *  TODO use prefix.cc web service to load from prefix short names (see implementation in EulerGUI)
 */
object PopulateRDFCache extends RDFCache
    with App {

  loadCommonVocabularies
  loadCommonFormSpecifications()
  RDFI18NLoader.loadFromGitHubRDFI18NTranslations()

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
        /* geo: , con: , <foaf_fr.n3>  TODO */
        makeUri("http://www.w3.org/2003/01/geo/wgs84_pos#") ::
        makeUri("http://www.w3.org/2000/10/swap/pim/contact#") ::
        // TODO use code to load all languages from github.com/jmvanel/rdf-i18n (see implementation in EulerGUI)
        makeUri("https://raw.githubusercontent.com/jmvanel/rdf-i18n/master/foaf/foaf.fr.ttl") ::
        makeUri("https://raw.githubusercontent.com/jmvanel/rdf-i18n/master/foaf/foaf.it.ttl") ::
        makeUri("https://raw.githubusercontent.com/jmvanel/rdf-i18n/master/rdfs/rdfs.fr.ttl") ::
        makeUri("https://raw.githubusercontent.com/jmvanel/rdf-i18n/master/rdfs/rdfs.it.ttl") ::
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

  /** load CommonForm Specifications from a well know place */
  def loadCommonFormSpecifications() {
    val all_form_specs = "https://raw.githubusercontent.com/jmvanel/semantic_forms/master/scala/forms/form_specs/specs.ttl"
    val from = new java.net.URL(all_form_specs).openStream()
    val form_specs_graph: Rdf#Graph = turtleReader.read(from, base = all_form_specs) getOrElse sys.error(s"couldn't read $all_form_specs")
    import deductions.runtime.abstract_syntax.FormSyntaxFactory._
    val formPrefix = Prefix("form", formVocabPrefix)
    /* Retrieving triple :
     * foaf: form:ontologyHasFormSpecification <foaf.form.ttl> . */
    val triples: Iterator[Rdf#Triple] = ops.find(form_specs_graph, ops.ANY, formPrefix("ontologyHasFormSpecification"), ops.ANY)
    val objects = for (triple <- triples) yield {
      triple.getObject
    }
    for (obj <- objects) {
      val from = new java.net.URL(obj.toString()).openStream()
      val form_spec_graph: Rdf#Graph = turtleReader.read(from, base = obj.toString()) getOrElse sys.error(
        s"couldn't read ${obj.toString()}")
      val r = rdfStore.rw(dataset, {
        rdfStore.appendToGraph(dataset, ops.makeUri("form_specs"), form_spec_graph)
      })
      println("Added form_spec " + obj)
    }
  }
}
