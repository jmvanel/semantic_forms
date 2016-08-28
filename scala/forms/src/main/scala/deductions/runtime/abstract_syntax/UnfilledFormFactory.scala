/**
 *
 */

package deductions.runtime.abstract_syntax

import scala.language.postfixOps

import org.w3.banana.RDF

import deductions.runtime.services.Configuration
import deductions.runtime.services.URIManagement

/** Factory for an Unfilled Form */
trait UnfilledFormFactory[Rdf <: RDF, DATASET]
    extends FormSyntaxFactory[Rdf, DATASET]
   	with FormConfigurationFactory[Rdf, DATASET]
    with Configuration
    with URIManagement {

  
  import ops._

  /**
   * create Form from a class URI,
   *  looking up for Form Configuration within RDF graph in this class
   */
  def createFormFromClass(classs: Rdf#URI,
    formSpecURI: String = "")
  	  (implicit graph: Rdf#Graph)
  	  : FormModule[Rdf#Node, Rdf#URI]#FormSyntax = {

    val (propsListInFormConfig, formConfig) =
      if (formSpecURI != "") {
        (propertiesListFromFormConfiguration(URI(formSpecURI)),
          URI(formSpecURI))
      } else {
        lookPropertiesListInConfiguration(classs)
      }
    val newId = // UnfilledFormFactory . 
        makeId
    if (propsListInFormConfig.isEmpty) {
      val props = fieldsFromClass(classs, graph)
      createFormDetailed(makeUri(newId), addRDFSLabelComment(props), classs, CreationMode)
    } else
      createFormDetailed(makeUri(newId), propsListInFormConfig.toSeq, classs,
        CreationMode, formConfig = formConfig)
  }

}