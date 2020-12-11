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
  def createFormFromClass(classOrForm: Rdf#URI,
    formSpecURI0: String = "" , request: HTTPrequest )
  	  (implicit graph: Rdf#Graph) : FormSyntax = {

    val formFromClass: FormSyntax = wrapInTransaction {
      lazy val itsOWLClass = isOWLClass(classOrForm)
      lazy val itsRDFSClass = isRDFSClass(classOrForm)
      lazy val itsFormSpecification = isFormSpecification(classOrForm)

      // if classs argument is not an owl:Class, check if it is a form:specification, then use it as formSpecURI
      val (formSpecURI, classs) = if (!itsOWLClass && !itsRDFSClass
        && itsFormSpecification)
        (fromUri(classOrForm), nullURI)
      else
        (formSpecURI0, classOrForm)
      val classFromSpecsOrGiven =
        if (formSpecURI =/= "" && classs == nullURI) {
          val classFromSpecs = lookClassInFormSpec(URI(formSpecURI), graph)
          uriNodeToURI(classFromSpecs)
        } else classs
      logger.info(s""">>> UnfilledFormFactory.createFormFromClass: formSpecURI=<$formSpecURI> , classOrForm <$classOrForm> =>
      classFromSpecsOrGiven <$classFromSpecsOrGiven>""")

      val newId = {
        val instanceURI = getFirstNonEmptyInMap(request.queryString, "subjecturi")
        if (instanceURI === "")
          makeId(request)
        else instanceURI
      }

      implicit val lang = request.getLanguage()
      val form = createFormDetailed(
        makeUri(newId),
        classFromSpecsOrGiven,
        CreationMode, nullURI, URI(formSpecURI), request)

      //      logger.info( s""">>>> createFormFromClass isFormSpec $itsFormSpecification
      //        itsOWLClass $itsOWLClass, itsRDFSClass $itsRDFSClass""" )
      //      if( itsOWLClass || itsRDFSClass )
      //        addExtraTypesFromHTTPrequest( form, request)
      //      else
      form
    }.getOrElse(FormSyntax(nullURI, Seq()))

    formFromClass
  }

//  def isFormSpecification(classOrForm: Rdf#URI)(implicit graph: Rdf#Graph): Boolean
//  = ! find( graph, classOrForm, rdf.typ, form("specification")).toList.isEmpty
//  def isOWLClass(classOrForm: Rdf#URI)(implicit graph: Rdf#Graph): Boolean
//  = ! find( graph, classOrForm, rdf.typ, owl.Class).toList.isEmpty

  // TODO put in reusable trait
  private def getFirstNonEmptyInMap(
    map: Map[String, Seq[String]],
    uri: String): String = {
    val uriArgs = map.getOrElse(uri, Seq())
    uriArgs.find { uri => uri  =/=  "" }.getOrElse("") . trim()
  }
}