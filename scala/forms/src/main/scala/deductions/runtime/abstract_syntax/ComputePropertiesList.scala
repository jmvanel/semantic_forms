package deductions.runtime.abstract_syntax

import org.w3.banana.RDF

/** intermediary data for form generation */
case class RawDataForForm[Rdf <: RDF](propertiesList: Seq[Rdf#Node],
    classs: Rdf#URI,
    subject: Rdf#Node,
    editable: Boolean = false,
    formURI: Option[Rdf#Node] = None,
    reversePropertiesList: Seq[Rdf#Node]= Seq() )

/** Step 1: compute properties List from Config, Subject, Class (in that order) */
trait ComputePropertiesList[Rdf <: RDF, DATASET] {
  self: FormSyntaxFactory[Rdf, DATASET] =>

  /** create Raw Data For Form from an instance (subject) URI,
   * and a Form Specification URI if not empty;
   * ( see form_specs/foaf.form.ttl for an example of form Specification)
   * it merges given properties from Config, with properties from Subject
   * and from Class (in this order). 
   * */
  protected def computePropertiesList(subject: Rdf#Node,
                            editable: Boolean = false, formuri: String)(implicit graph: Rdf#Graph):
                            RawDataForForm[Rdf] = {

    val classs = classFromSubject(subject) // TODO several classes
    val (propsFromConfig: Seq[Rdf#URI], formConfiguration) =
      computePropsFromConfig(classs, formuri)

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

  /** look for Properties list form Configuration in given URI or else in TDB from given classs URI */
  private def computePropsFromConfig(classs: Rdf#URI,
      formuri: String)(implicit graph: Rdf#Graph): (Seq[Rdf#URI], Rdf#Node) =
    if (formuri == "")
      lookPropertiesListInConfiguration(classs)
    else {
      lookPropertiesListFromDatabaseOrDownload(formuri)
    }
  
  
  private def classFromSubject(subject: Rdf#Node)(implicit graph: Rdf#Graph)
  = {
    getHeadOrElse(subject, rdf.typ)
  }
}
