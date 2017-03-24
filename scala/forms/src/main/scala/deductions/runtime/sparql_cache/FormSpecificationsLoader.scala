package deductions.runtime.sparql_cache

import org.w3.banana.RDF

import deductions.runtime.jena.ImplementationSettings
import deductions.runtime.jena.RDFCache
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import deductions.runtime.services.DefaultConfiguration
import deductions.runtime.utils.RDFPrefixes
import deductions.runtime.DependenciesForApps
import deductions.runtime.utils.HTTPHelpers

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
    if (args.size == 0)
      loadCommonFormSpecifications()
    else
      loadFormSpecification(args(0))
  println(s"DONE load Common Form Specifications in named graph <$formSpecificationsGraphURI>")
  close(dataset)
}

trait FormSpecificationsLoaderTrait[Rdf <: RDF, DATASET]
    extends RDFCacheAlgo[Rdf, DATASET]
    with RDFPrefixes[Rdf]
    with SitesURLForDownload
        with HTTPHelpers {

  import ops._
  import rdfStore.transactorSyntax._

  val all_form_specs_document = githubcontent +
  "/jmvanel/semantic_forms/master/scala/forms/form_specs/specs.ttl"
  val formSpecificationsGraphURI = URI( 
      // all_form_specs_document +"#" ) //
      "urn:form_specs")

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
    loadFormSpecification(all_form_specs_document)
  }

  /** non TRANSACTIONAL */
  def loadFormSpecification(form_specs: String) {
    try {
      setTimeoutsFromConfig()

      // NOTE: don't know why this triggers sometimes: [error] java.net.SocketTimeoutException: connect timed out
      // val from = new java.net.URL(form_specs).openStream()

      val form_specs_graph: Rdf#Graph =
//    turtleReader.read(from, base = form_specs) 
        rdfLoader.load(new java.net.URL(form_specs)) . getOrElse (sys.error(
          s"couldn't read form_specs <$form_specs>"))

      val formPrefix = form
      /* Retrieving such triples:
       * foaf: form:ontologyHasFormSpecification <foaf.form.ttl> . */
      val triples: Iterator[Rdf#Triple] = find(form_specs_graph, ANY, formPrefix("ontologyHasFormSpecification"), ANY)
      val formSpecifications = for (triple <- triples) yield triple.objectt

      for (formSpecification <- formSpecifications) {
        try {
//          val from = new java.net.URL(formSpecification.toString()).openStream()
          val form_spec_graph: Rdf#Graph =
            // turtleReader.read(from, base = formSpecification.toString())
            rdfLoader.load(new java.net.URL(nodeToString(formSpecification))) . getOrElse (sys.error(
            s"couldn't read form Specification <${formSpecification.toString()}>"))
          val ret = wrapInTransaction {
            rdfStore.appendToGraph(dataset, formSpecificationsGraphURI, form_spec_graph)
          }
          println(s"Added form_spec <$formSpecification> in named graph <$formSpecificationsGraphURI> (${form_spec_graph.size} triples) $ret")
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