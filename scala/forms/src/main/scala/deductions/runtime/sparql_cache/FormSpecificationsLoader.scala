package deductions.runtime.sparql_cache

import deductions.runtime.jena.RDFStoreLocalJena1Provider
import deductions.runtime.jena.JenaHelpers
import org.w3.banana.Prefix

/**
 * @author jmv
 */
object FormSpecificationsLoader extends RDFCache with App
    with RDFStoreLocalJena1Provider
    with JenaHelpers {

  import ops._
  import rdfStore.transactorSyntax._
  import rdfStore.graphStoreSyntax._

  val githubcontent = "https://raw.githubusercontent.com"
  loadCommonFormSpecifications()

  def loadCommonFormSpecifications() {
    val all_form_specs = githubcontent +
      "/jmvanel/semantic_forms/master/scala/forms/form_specs/specs.ttl"
    val from = new java.net.URL(all_form_specs).openStream()
    val form_specs_graph: Rdf#Graph = turtleReader.read(from, base = all_form_specs) getOrElse sys.error(s"couldn't read $all_form_specs")
    import deductions.runtime.abstract_syntax.FormSyntaxFactory._
    val formPrefix = Prefix("form", formVocabPrefix)
    /* Retrieving triple :
     * foaf: form:ontologyHasFormSpecification <foaf.form.ttl> . */
    val triples: Iterator[Rdf#Triple] = ops.find(form_specs_graph, ops.ANY, formPrefix("ontologyHasFormSpecification"), ops.ANY)
    val objects = for (triple <- triples) yield {
      triple._3 // getObject
    }
    for (obj <- objects) {
      val from = new java.net.URL(obj.toString()).openStream()
      val form_spec_graph: Rdf#Graph = turtleReader.read(from, base = obj.toString()) getOrElse sys.error(
        s"couldn't read ${obj.toString()}")
      val r = dataset.rw({
        dataset.appendToGraph(ops.makeUri("form_specs"), form_spec_graph)
      })
      println("Added form_spec " + obj)
    }
  }
}