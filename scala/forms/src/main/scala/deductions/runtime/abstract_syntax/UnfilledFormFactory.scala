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
   *  looking up for Form Configuration within RDF graph in this class
   */
  def createFormFromClass(classs: Rdf#URI,
    formSpecURI: String = "" , request: HTTPrequest= HTTPrequest() )
  	  (implicit graph: Rdf#Graph) : FormSyntax = {

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