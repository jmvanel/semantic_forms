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

  if (args.size == 0)
    loadCommonFormSpecifications()
  else
    loadFormSpecifications(args(0))
}

trait FormSpecificationsLoaderTrait[Rdf <: RDF, DATASET]
    extends RDFCacheAlgo[Rdf, DATASET]
    with RDFPrefixes[Rdf]
    with SitesURLForDownload {

  import ops._
  import rdfStore.transactorSyntax._

  val formSpecificationsGraph = URI("urn:form_specs")

  /** TRANSACTIONAL */
  def resetCommonFormSpecifications() {
    rdfStore.rw( dataset, {
      rdfStore.removeGraph(dataset, formSpecificationsGraph)
    })
  }

  /**
   * load Common Form Specifications from scala/forms/form_specs/specs.ttl
   *  on in project jmvanel/semantic_forms on github;
   *  non TRANSACTIONAL
   */
  def loadCommonFormSpecifications() {
    val all_form_specs = githubcontent +
      "/jmvanel/semantic_forms/master/scala/forms/form_specs/specs.ttl"
    loadFormSpecifications(all_form_specs)
  }

  /** non TRANSACTIONAL */
  def loadFormSpecifications(form_specs: String) {
    try {
      val from = new java.net.URL(form_specs).openStream()
      val form_specs_graph: Rdf#Graph =
        turtleReader.read(from, base = form_specs) getOrElse sys.error(
          s"couldn't read $form_specs")
      //    import deductions.runtime.abstract_syntax.FormSyntaxFactory._
      val formPrefix = form
      /* Retrieving triple :
     * foaf: form:ontologyHasFormSpecification <foaf.form.ttl> . */
      val triples: Iterator[Rdf#Triple] = find(form_specs_graph, ANY, formPrefix("ontologyHasFormSpecification"), ANY)
      val objects = for (triple <- triples) yield {
        triple._3 // getObject
      }
      for (obj <- objects) {
        try {
          val from = new java.net.URL(obj.toString()).openStream()
          val form_spec_graph: Rdf#Graph = turtleReader.read(from, base = obj.toString()) getOrElse sys.error(
            s"couldn't read ${obj.toString()}")
          rdfStore.rw( dataset, {
            rdfStore.appendToGraph(dataset, formSpecificationsGraph, form_spec_graph)
          })
          println("Added form_spec " + obj)
        } catch {
          case e: Exception =>
            System.err.println(s"""!!!! Error in loadFormSpecifications:
            $obj
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