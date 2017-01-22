/**
 *
 */

package deductions.runtime.abstract_syntax

import scala.language.postfixOps

import org.w3.banana.RDF

import deductions.runtime.services.Configuration
import deductions.runtime.services.URIManagement
import deductions.runtime.utils.HTTPrequest

/** Factory for an Unfilled Form */
trait UnfilledFormFactory[Rdf <: RDF, DATASET]
    extends FormSyntaxFactory[Rdf, DATASET]
   	with FormConfigurationFactory[Rdf, DATASET]
    with URIManagement {

  import ops._

  /**
   * create Form from a class URI,
   *  looking up for Form Configuration within RDF graph
   */
  def createFormFromClass(classs: Rdf#URI,
    formSpecURI0: String = "" , request: HTTPrequest= HTTPrequest() )
  	  (implicit graph: Rdf#Graph) : FormSyntax = {

    // if classs argument is not an owl:Class, check if it is a form:specification, then use it as formSpecURI
    val checkIsOWLClass = ops.find( graph, classs, rdf.typ, owl.Class)
    val checkIsRDFSClass = ops.find( graph, classs, rdf.typ, rdfs.Class)
    val checkIsFormSpec = ops.find( graph, classs, rdf.typ, form("specification") )
    val formSpecURI = if( checkIsOWLClass.isEmpty && checkIsRDFSClass.isEmpty
        && ! checkIsFormSpec.isEmpty )
      fromUri(classs) else formSpecURI0

    val (propsListInFormConfig, formConfig) =
      if (formSpecURI != "") {
        (propertiesListFromFormConfiguration(URI(formSpecURI)),
          URI(formSpecURI))
      } else {
        lookPropertiesListInConfiguration(classs)
      }

    println(s">>> UnfilledFormFactory.createFormFromClass: formSpecURI <$formSpecURI> classs <$classs>")
    val classFromSpecsOrGiven =
      if (formSpecURI != "" && classs == URI("") ) {
        val classFromSpecs = lookClassInFormSpec( URI(formSpecURI), graph)
        uriNodeToURI(classFromSpecs)
      } else classs
    println(s">>> UnfilledFormFactory.createFormFromClass: classFromSpecsOrGiven <$classFromSpecsOrGiven>")

    val newId = makeId(request)
    if (propsListInFormConfig.isEmpty) {
      val props = fieldsFromClass(classFromSpecsOrGiven, graph).propertiesList
      createFormDetailed(makeUri(newId), addRDFSLabelComment(props), classFromSpecsOrGiven, CreationMode)
    } else
      createFormDetailed(makeUri(newId), propsListInFormConfig.toSeq, classFromSpecsOrGiven,
        CreationMode, formConfig = formConfig)
  }

}