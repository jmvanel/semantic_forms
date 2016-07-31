package deductions.runtime.abstract_syntax

import org.w3.banana.RDF

case class RawDataForForm[Rdf <: RDF](propertiesList: Seq[Rdf#Node], classs: Rdf#URI)

/** Step 1: compute properties List from Config, Subject, Class (in that order) */
trait ComputePropertiesList[Rdf <: RDF, DATASET] {
  self: FormSyntaxFactory[Rdf, DATASET] =>

   /** create Raw Data For Form from an instance (subject) URI,
     * and a Form Specification URI
     * ( see form_specs/foaf.form.ttl for an example )
     * */
    def computePropertiesList(subject: Rdf#Node,
                            editable: Boolean = false, formuri: String)(implicit graph: Rdf#Graph): RawDataForForm[Rdf] = {

    val classs = classFromSubject(subject) // TODO several classes
    val propsFromConfig: Seq[Rdf#URI] = computePropsFromConfig(classs, formuri)
    computePropertiesListWithList(subject, editable, propsFromConfig)
  }

    /** create Raw Data For Form from an instance (subject) URI,
     * and a properties list (typically From Config);
     * it merges given properties with properties From Config, From Subject
     * and From Class.
     * */
    def computePropertiesListWithList(subject: Rdf#Node,
                            editable: Boolean = false,
                            propsFromConfig: Seq[Rdf#URI])(implicit graph: Rdf#Graph) = {
    val classs = classFromSubject(subject) // TODO several classes
    val propsFromSubject = fieldsFromSubject(subject, graph)
    val propsFromClass: Seq[Rdf#Node] =
      if (editable) {
        fieldsFromClass(classs, graph)
      } else Seq()
    val propertiesList0 = (propsFromConfig ++ propsFromSubject ++ propsFromClass).distinct
    val propertiesList = addRDFSLabelComment(propertiesList0)
    RawDataForForm(propertiesList, classs)
  }

  private def computePropsFromConfig(classs: Rdf#URI, formuri: String)(implicit graph: Rdf#Graph): Seq[Rdf#URI] =
    if (formuri == "")
      lookPropertieslistFormInConfiguration(classs)._1
    else {
      lookPropertiesListFromDatabaseOrDownload(formuri)._1
    }
  
  
  private def classFromSubject(subject: Rdf#Node)
  (implicit graph: Rdf#Graph)
  = {
    getHeadOrElse(subject, rdf.typ)
  }
}
