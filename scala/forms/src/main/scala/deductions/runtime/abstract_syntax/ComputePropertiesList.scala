package deductions.runtime.abstract_syntax

import org.w3.banana.RDF

case class RawDataForForm[Rdf <: RDF](propertiesList: Seq[Rdf#Node], classs: Rdf#URI)

/** Step 1: compute properties List from Config, Subject, Class (in that order) */
trait ComputePropertiesList[Rdf <: RDF, DATASET] {
  self: FormSyntaxFactory[Rdf, DATASET] =>

  def computePropertiesList(subject: Rdf#Node,
                            editable: Boolean = false, formuri: String)(implicit graph: Rdf#Graph): RawDataForForm[Rdf] = {

    val classs = classFromSubject(subject) // TODO several classes
    val propsFromConfig: Seq[Rdf#URI] = computePropsFromConfig(classs, formuri)
    computePropertiesListFromConfig(subject,
                            editable,
                            propsFromConfig)
  }

  def computePropertiesListFromConfig(subject: Rdf#Node,
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
}
