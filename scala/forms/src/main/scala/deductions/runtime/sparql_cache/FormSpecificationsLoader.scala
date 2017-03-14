package deductions.runtime.sparql_cache

import org.w3.banana.RDF

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.jena.RDFCache
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import deductions.runtime.services.DefaultConfiguration
import deductions.runtime.utils.RDFPrefixes
import deductions.runtime.DependenciesForApps

/**
 * @author jmv
 */

/** Form Specifications Loader App */
object FormSpecificationsLoader
    extends {
      override val config = new DefaultConfiguration {
        override val useTextQuery = false
      }
    } with DependenciesForApps
    with FormSpecificationsLoaderTrait[ImplementationSettings.Rdf, ImplementationSettings.DATASET] {

  import config._

  resetCommonFormSpecifications()
  val ret = wrapInTransaction{
    if (args.size == 0)
      loadCommonFormSpecifications()
    else
      loadFormSpecifications(args(0))}
  println(s"DONE load Common Form Specifications in named graph <$formSpecificationsGraphURI> : $ret")
  close(dataset)
}

trait FormSpecificationsLoaderTrait[Rdf <: RDF, DATASET]
    extends RDFCacheAlgo[Rdf, DATASET]
    with RDFPrefixes[Rdf]
    with SitesURLForDownload {

  import ops._
  import rdfStore.transactorSyntax._

  val formSpecificationsGraphURI = URI("urn:form_specs")

  /** TRANSACTIONAL */
  def resetCommonFormSpecifications() {
    val ret = wrapInTransaction {
      rdfStore.removeGraph(dataset, formSpecificationsGraphURI)
    }
    println(s"DONE reset Common Form Specifications in named graph <$formSpecificationsGraphURI> : $ret")
  }

  /**
   * load Common Form Specifications from scala/forms/form_specs/specs.ttl
   *  on in project jmvanel/semantic_forms on github;
   *  non TRANSACTIONAL
   */
  def loadCommonFormSpecifications() {
    val all_form_specs_document = githubcontent +
      "/jmvanel/semantic_forms/master/scala/forms/form_specs/specs.ttl"
    loadFormSpecifications(all_form_specs_document)
  }

  /** non TRANSACTIONAL */
  def loadFormSpecifications(form_specs: String) {
    try {
      val from = new java.net.URL(form_specs).openStream()
      val form_specs_graph: Rdf#Graph =
        turtleReader.read(from, base = form_specs) getOrElse sys.error(
          s"couldn't read form_specs <$form_specs>")

      val formPrefix = form
      /* Retrieving such triples:
       * foaf: form:ontologyHasFormSpecification <foaf.form.ttl> . */
      val triples: Iterator[Rdf#Triple] = find(form_specs_graph, ANY, formPrefix("ontologyHasFormSpecification"), ANY)
      val formSpecifications = for (triple <- triples) yield triple.objectt

      for (formSpecification <- formSpecifications) {
        try {
          val from = new java.net.URL(formSpecification.toString()).openStream()
          val form_spec_graph: Rdf#Graph = turtleReader.read(from, base = formSpecification.toString()) getOrElse sys.error(
            s"couldn't read form Specification <${formSpecification.toString()}>")
          rdfStore.rw( dataset, {
            rdfStore.appendToGraph(dataset, formSpecificationsGraphURI, form_spec_graph)
          })
          println(s"Added form_spec <$formSpecification> in named graph <$formSpecificationsGraphURI> (${form_spec_graph.size} triples)")
        } catch {
          case e: Exception =>
            System.err.println(s"""!!!! Error in loadFormSpecifications:
            $formSpecification
            $e
            ${e.printStackTrace()}""")
        }
      }
    } catch {
      case e: Exception =>
        System.err.println(s"""!!!! Error in loadFormSpecifications: load form_specs <$form_specs>
            $e
            ${e.printStackTrace()}""")
    }
  }
}