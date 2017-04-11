/**
 *
 */

package deductions.runtime.abstract_syntax

import scala.language.postfixOps

import org.w3.banana.RDF

import deductions.runtime.utils.URIManagement
import deductions.runtime.utils.HTTPrequest

/** Factory for an Unfilled Form */
trait UnfilledFormFactory[Rdf <: RDF, DATASET]
    extends FormSyntaxFactory[Rdf, DATASET]
   	with FormSpecificationFactory[Rdf, DATASET]
    with URIManagement {

  import ops._

  /**
   * create Form from a class URI,
   *  looking up for Form Configuration within RDF graph
   *  
   *  TODO check URI arguments: they must be valid, absolute
   *  TODO return Try
   *  TODO use HTTP request param graphURI
   */
  def createFormFromClass(classe: Rdf#URI,
    formSpecURI0: String = "" , request: HTTPrequest= HTTPrequest() )
  	  (implicit graph: Rdf#Graph, lang:String) : FormSyntax = {

    wrapInReadTransaction {
//      println( s">>>> createFormFromClass <$classe> " + ops.getSubjects(graph, rdf.typ, foaf.Person ) )

    // if classs argument is not an owl:Class, check if it is a form:specification, then use it as formSpecURI
    val checkIsOWLClass = find( graph, classe, rdf.typ, owl.Class)
    val checkIsRDFSClass = find( graph, classe, rdf.typ, rdfs.Class)
    val checkIsFormSpec = find( graph, classe, rdf.typ, form("specification") ) . toList
//    println( s">>>> createFormFromClass checkIsFormSpec $checkIsFormSpec" )

    val ( formSpecURI, classs ) = if( checkIsOWLClass.isEmpty && checkIsRDFSClass.isEmpty
        && ! checkIsFormSpec.isEmpty )
      (fromUri(classe), nullURI ) else (formSpecURI0, classe)

    val (propsListInFormConfig, formConfig) =
      if (formSpecURI != "") {
        (propertiesListFromFormConfiguration(URI(formSpecURI)),
          URI(formSpecURI))
      } else {
        lookPropertiesListInConfiguration(classe)
      }

    logger.info( s""">>> UnfilledFormFactory.createFormFromClass: formSpecURI <$formSpecURI> classs <$classs>
    		props List In Form Config size ${propsListInFormConfig.size}""")
    val classFromSpecsOrGiven =
      if (formSpecURI != "" && classs == nullURI ) {
        val classFromSpecs = lookClassInFormSpec( URI(formSpecURI), graph)
        uriNodeToURI(classFromSpecs)
      } else classs
    println(s">>> UnfilledFormFactory.createFormFromClass: classFromSpecsOrGiven <$classFromSpecsOrGiven>")

    val instanceURI = getFirstNonEmptyInMap(request.queryString, "subjecturi")
    val newId = if (instanceURI == "")
      makeId(request)
    else
      instanceURI

    // TODO properties List argument IS NOT USED !!!!
    if (propsListInFormConfig.isEmpty) {
      val props = fieldsFromClass(classFromSpecsOrGiven, graph).propertiesList
      createFormDetailed(makeUri(newId), addRDFSLabelComment(props), classFromSpecsOrGiven, CreationMode)
    } else
      createFormDetailed(makeUri(newId), propsListInFormConfig.toSeq, classFromSpecsOrGiven,
        CreationMode, formConfig = formConfig)
  } . getOrElse( FormSyntax( nullURI, Seq() ) )
  }

  // TODO put in reusable trait
  private def getFirstNonEmptyInMap(
    map: Map[String, Seq[String]],
    uri: String): String = {
    val uriArgs = map.getOrElse(uri, Seq())
    uriArgs.find { uri => uri != "" }.getOrElse("") . trim()
  }
}