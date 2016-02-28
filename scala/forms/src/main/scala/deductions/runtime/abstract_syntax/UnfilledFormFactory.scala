/**
 *
 */

package deductions.runtime.abstract_syntax

import java.net.InetAddress
import java.net.URLEncoder
import scala.language.postfixOps
import org.w3.banana.RDF
import deductions.runtime.services.Configuration
import deductions.runtime.services.DefaultConfiguration
import deductions.runtime.utils.URIHelpers
import java.net.URLDecoder
import deductions.runtime.services.URIManagement

///** moved to package services, and rename to URIManagement
// * @author j.m. Vanel
// */
//object UnfilledFormFactory extends DefaultConfiguration
//with URIHelpers {
//
//	def makeId: String = {
//    makeId(instanceURIPrefix)
//  }
//
//  /** make URI From String, if not already an absolute URI */
//  def makeURIFromString(objectStringFromUser: String): String = {
//    if (isAbsoluteURI(objectStringFromUser))
//      objectStringFromUser
//    else {
//      instanceURIPrefix +
//        // urlencode takes care of other forbidden character in "candidate" URI
//        URLEncoder.encode(objectStringFromUser.replaceAll(" ", "_"), "UTF-8")
//    }
//  }
//
//    /** make String From URI */
//  def makeStringFromURI( uri: String): String = {
//    lastSegment( URLDecoder.decode( uri, "UTF-8" ) ).replaceAll("_", " ")
//  }
//
//  /** make a unique Id with given prefix, currentTimeMillis() and nanoTime() */
//  private def makeId(instanceURIPrefix: String): String = {
//      instanceURIPrefix + System.currentTimeMillis() + "-" + System.nanoTime() // currentId = currentId + 1
//  }
//
//  val instanceURIPrefix: String = {
//    val hostNameUsed =
//      if (useLocalHostPrefixForURICreation) {
//        "http://" + InetAddress.getLocalHost().getHostName()
//        // TODO : get the actual port
//      } else defaultInstanceURIHostPrefix
//    hostNameUsed +  ":" + serverPort + "/" + relativeURIforCreatedResourcesByForm
//  }
//
//}

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
        lookPropertieslistFormInConfiguration(classs)
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