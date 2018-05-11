/**
 *
 */

package deductions.runtime.abstract_syntax

import deductions.runtime.utils.URIManagement
import deductions.runtime.core.HTTPrequest
import org.w3.banana.RDF

import scala.language.postfixOps
import scalaz._
import Scalaz._

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
   *  
   *  Transaction inside (RW)
   */
  def createFormFromClass(classe: Rdf#URI,
    formSpecURI0: String = "" , request: HTTPrequest= HTTPrequest() )
  	  (implicit graph: Rdf#Graph, lang:String) : FormSyntax = {

    wrapInTransaction {
    // if classs argument is not an owl:Class, check if it is a form:specification, then use it as formSpecURI
    val checkIsOWLClass = find( graph, classe, rdf.typ, owl.Class)
    val checkIsRDFSClass = find( graph, classe, rdf.typ, rdfs.Class)
    val checkIsFormSpec = find( graph, classe, rdf.typ, form("specification") ) . toList
    logger.debug( s">>>> createFormFromClass checkIsFormSpec $checkIsFormSpec" )

    val ( formSpecURI, classs ) = if( checkIsOWLClass.isEmpty && checkIsRDFSClass.isEmpty
        && ! checkIsFormSpec.isEmpty )
      (fromUri(classe), nullURI ) else (formSpecURI0, classe)

    val classFromSpecsOrGiven =
      if (formSpecURI  =/=  "" && classs == nullURI ) {
        val classFromSpecs = lookClassInFormSpec( URI(formSpecURI), graph)
        uriNodeToURI(classFromSpecs)
      } else classs
    logger.info(s">>> UnfilledFormFactory.createFormFromClass: formSpecURI=<> , class <$classe> => classFromSpecsOrGiven <$classFromSpecsOrGiven>")

    val instanceURI = getFirstNonEmptyInMap(request.queryString, "subjecturi")
    val newId = if (instanceURI === "")
      makeId(request)
    else
      instanceURI

      createFormDetailed(makeUri(newId),
        classFromSpecsOrGiven,
        CreationMode, nullURI, URI(formSpecURI0) )
  } . getOrElse( FormSyntax( nullURI, Seq() ) )
  }

  // TODO put in reusable trait
  private def getFirstNonEmptyInMap(
    map: Map[String, Seq[String]],
    uri: String): String = {
    val uriArgs = map.getOrElse(uri, Seq())
    uriArgs.find { uri => uri  =/=  "" }.getOrElse("") . trim()
  }
}