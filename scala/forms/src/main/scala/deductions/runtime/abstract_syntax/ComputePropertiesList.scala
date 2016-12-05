package deductions.runtime.abstract_syntax

import org.w3.banana.RDF
import scala.util.Try
import scala.util.Success
import scala.collection.mutable.HashMap

/** intermediary data for form generation:  properties' List, etc */
case class RawDataForForm[Rdf <: RDF](
    propertiesList: Seq[Rdf#Node],
    classs: Rdf#Node, // URI,
    subject: Rdf#Node,
    editable: Boolean = false,
    formURI: Option[Rdf#Node] = None,
    reversePropertiesList: Seq[Rdf#Node]= Seq(),
    propertiesGroups: collection.Map[Rdf#Node, RawDataForForm[Rdf]] = collection.Map[Rdf#Node, RawDataForForm[Rdf]]()
)

/** Step 1: compute properties List from Config, Subject, Class (in that order) */
trait ComputePropertiesList[Rdf <: RDF, DATASET] {
  self: FormSyntaxFactory[Rdf, DATASET] =>

  /** look for Properties list from form spec in given URI or else in TDB from given class URI
   *  @return (propertiesList, formConfiguration, tryClass) */
  private def computePropsFromConfig(classs: Rdf#URI,
      formuri: String)(implicit graph: Rdf#Graph): (Seq[Rdf#URI], Rdf#Node, Try[Rdf#Node]) =
    if (formuri == "") {
      val (propertiesList, formConfiguration) = lookPropertiesListInConfiguration(classs)
            (propertiesList, formConfiguration, Success(classs))
    } else {
      val (propertiesList, formConfiguration, tryGraph) = lookPropertiesListFromDatabaseOrDownload(formuri)
      val tryClass = tryGraph . map { gr => 
         lookClassInFormSpec( ops.URI(formuri), gr)
      }
//      (propertiesList, formConfiguration)
      (propertiesList, formConfiguration, tryClass)
    }

  /** create Raw Data For Form from an instance (subject) URI,
   * and possibly a Form Specification URI if URI is not <> ;
   * ( see form_specs/foaf.form.ttl for an example of form Specification)
   * 
   * it merges given properties from Config, with properties from Subject
   * and from Class (in this order).
   * @return a RawDataForForm data structure
   * */
  protected def computePropertiesList(subject: Rdf#Node,
                            editable: Boolean = false, formuri: String)(implicit graph: Rdf#Graph):
                            RawDataForForm[Rdf] = {
      
    val classOfSubject = classFromSubject(subject) // TODO several classes

    val (propsFromConfig: Seq[Rdf#URI], formConfiguration, tryClass ) =
      computePropsFromConfig(classOfSubject, formuri)

    val classs = if( classOfSubject == ops.URI("") && formuri != ops.URI("")) {
      println(s">>>> computePropertiesList $tryClass")
    	uriNodeToURI(tryClass.getOrElse(ops.URI("")))
    } else classOfSubject
    
    val propsFromSubject = fieldsFromSubject(subject, graph)
    val propsFromClass: Seq[Rdf#Node] =
      if (editable) {
        fieldsFromClass(classs, graph)
      } else Seq()

    val propertiesList0 = (propsFromConfig ++ propsFromSubject ++ propsFromClass).distinct
    val propertiesList = addRDFSLabelComment(propertiesList0)
    val reversePropertiesList = reversePropertiesListFromFormConfiguration(formConfiguration)
    RawDataForForm(propertiesList, classs, subject, editable,
        formuri match { case "" => None
        case uri => Some(ops.URI(uri)) },
        reversePropertiesList)
  }
  
  
  private def classFromSubject(subject: Rdf#Node)(implicit graph: Rdf#Graph)
  = {
    getHeadOrElse(subject, rdf.typ)
  }
}
