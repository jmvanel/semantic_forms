package deductions.runtime.abstract_syntax

import org.w3.banana.RDF
import scala.util.Try
import scala.util.Success
import scala.collection.mutable.HashMap

/** intermediary data for form generation:  properties' List, etc */
case class RawDataForForm[Node]( //  <: RDF.Node](
    propertiesList: Seq[Node],
    classs: Node, // URI,
    subject: Node,
    editable: Boolean = false,
    formURI: Option[Node] = None,
    reversePropertiesList: Seq[Node] = Seq(),
    propertiesGroups: collection.Map[Node, RawDataForForm[Node]] = collection.Map[Node, RawDataForForm[Node]]()) {
  def setSubject(subject: Node, editable: Boolean): RawDataForForm[Node] = {
    val propertiesGroupsWithSubject = propertiesGroups.map {
      case (node, rawDataForForm) => (node,
        rawDataForForm.setSubject(subject, editable))
    }
    RawDataForForm[Node](propertiesList, classs, subject, editable, formURI, reversePropertiesList,
      propertiesGroupsWithSubject)
  }
}

/** Step 1 of form generation: compute properties List from Config, Subject, Class (in that order) */
trait ComputePropertiesList[Rdf <: RDF, DATASET] {
  self: FormSyntaxFactory[Rdf, DATASET] =>
  import ops._

  /**
   * create Raw Data For Form from an instance (subject) URI,
   * and possibly a Form Specification URI if URI is not <> ;
   * ( see form_specs/foaf.form.ttl for an example of form Specification)
   *
   * it merges given properties from Config, with properties from Subject
   * and from Class (in this order).
   * @return a RawDataForForm data structure
   */
  protected def computePropertiesList(subject: Rdf#Node,
                                      editable: Boolean = false, formuri: String)(implicit graph: Rdf#Graph): RawDataForForm[Rdf#Node] = {

    val classOfSubject = classFromSubject(subject) // TODO several classes

    val (propsFromConfig, formConfiguration, tryClass) =
      computePropsFromConfig(classOfSubject, formuri)

    val classs = if (classOfSubject == ops.URI("") && formuri != ops.URI("")) {
      println(s">>>> computePropertiesList $tryClass")
      uriNodeToURI(tryClass.getOrElse(ops.URI("")))
    } else classOfSubject
    println(s">>> computePropsFromConfig( $classs) => formConfiguration=$formConfiguration, $propsFromConfig")

    val propsFromSubject = fieldsFromSubject(subject, graph)
    val propsFromClass = {
      //      if (editable) {
      val ff = fieldsFromClass(classs, graph)
      ff.setSubject(subject, editable)
      //      } else Seq()
    }
    val propertiesList0 = (propsFromConfig ++ propsFromSubject ++ propsFromClass.propertiesList).distinct
    val propertiesList = addRDFSLabelComment(propertiesList0)
    val reversePropertiesList = reversePropertiesListFromFormConfiguration(formConfiguration)

    def makeRawDataForForm(propertiesGroups: collection.Map[Rdf#Node, RawDataForForm[Rdf#Node]]) =
      RawDataForForm[Rdf#Node](propertiesList, classs, subject, editable,
        formuri match { case "" => None; case uri => Some(URI(uri)) },
        reversePropertiesList,
        propertiesGroups = propertiesGroups)

    val rawDataFromSpecif = RawDataForForm[Rdf#Node](
      propsFromConfig, classs, subject, editable)

    return makeRawDataForForm(
      propsFromClass.propertiesGroups
        + (formConfiguration -> rawDataFromSpecif))
  }

  /**
   * look for Properties list from form spec in given URI or else in TDB from given class URI
   *  @return (propertiesList, formConfiguration, tryClass)
   */
  private def computePropsFromConfig(classs: Rdf#URI,
                                     formuri: String)(implicit graph: Rdf#Graph): (Seq[Rdf#URI], Rdf#Node, Try[Rdf#Node]) =
    if (formuri == "") {
      val (propertiesList, formConfiguration) = lookPropertiesListInConfiguration(classs)
      (propertiesList, formConfiguration, Success(classs))
    } else {
      val (propertiesList, formConfiguration, tryGraph) = lookPropertiesListFromDatabaseOrDownload(formuri)
      val tryClass = tryGraph.map { gr =>
        lookClassInFormSpec(ops.URI(formuri), gr)
      }
      (propertiesList, formConfiguration, tryClass)
    }

  private def classFromSubject(subject: Rdf#Node)(implicit graph: Rdf#Graph) = {
    getHeadOrElse(subject, rdf.typ)
  }
}
