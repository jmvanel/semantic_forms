/**
 *
 */

package deductions.runtime.abstract_syntax

import scala.language.postfixOps
import org.w3.banana.RDF
import deductions.runtime.services.Configuration
import java.net.InetAddress
import deductions.runtime.services.DefaultConfiguration

/**
 * @author j.m. Vanel
 *
 */
object UnfilledFormFactory extends DefaultConfiguration {
  /** make a unique Id with given prefix, currentTimeMillis() and nanoTime() */
  def makeId(instanceURIPrefix: String): String = {
      instanceURIPrefix + System.currentTimeMillis() + "-" + System.nanoTime() // currentId = currentId + 1
  }
}

/** Factory for an Unfilled Form */
trait UnfilledFormFactory[Rdf <: RDF, DATASET]
    extends FormSyntaxFactory[Rdf, DATASET]
   	with FormConfigurationFactory[Rdf, DATASET]
    with Configuration {

  val instanceURIPrefix: String = // defaultInstanceURIPrefix
    if( useLocalHostPrefixForURICreation ) {
      val localHost = InetAddress.getLocalHost().getHostName()
      localHost + "/" + relativeURIforCreatedResourcesByForm
    } else defaultInstanceURIHostPrefix
      
  import ops._

  /**
   * create Form from a class URI,
   *  looking up for Form Configuration within RDF graph in this class
   */
  def createFormFromClass(classs: Rdf#URI,
    formSpecURI: String = "")
  	  (implicit graph: Rdf#Graph)
  	  : FormModule[Rdf#Node, Rdf#URI]#FormSyntax = {
//	  val formConfiguration = this

    val (propsListInFormConfig, formConfig) =
      if (formSpecURI != "") {
        (propertiesListFromFormConfiguration(URI(formSpecURI)),
          URI(formSpecURI))
      } else {
        lookPropertieslistFormInConfiguration(classs)
      }
    val newId = makeId
    if (propsListInFormConfig.isEmpty) {
      val props = fieldsFromClass(classs, graph)
      createFormDetailed(makeUri(newId), addRDFSLabelComment(props), classs, CreationMode)
    } else
      createFormDetailed(makeUri(newId), propsListInFormConfig.toSeq, classs,
        CreationMode, formConfig = formConfig)
  }

  def makeId: String = {
    UnfilledFormFactory.makeId(instanceURIPrefix)
  }
}