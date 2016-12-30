package deductions.runtime.abstract_syntax

import scala.util.Success
import scala.util.Try

import org.w3.banana.RDF

/** intermediary data for form generation:  properties' List, etc */
case class RawDataForForm[Node]( //  <: RDF.Node](
    propertiesList: Seq[Node],
    classs: Node,
    subject: Node,
    editable: Boolean = false,
    formURI: Option[Node] = None,
    reversePropertiesList: Seq[Node] = Seq(),
    /** properties Groups come from multiple super-classes */
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
   * ( see form_specs/foaf.form.ttl for an example of form Specification);
   *
   * it merges given properties from Config, with properties from Subject
   * and from Class (in this order).
   * 
   * The class is either:
   * - given as argument
   * - inferred from subject
   * - inferred from form specification
   *
   * @return a RawDataForForm data structure
   */
  protected def computePropertiesList(subject: Rdf#Node,
                                      editable: Boolean = false,
                                      formuri: String,
                                      classs: Rdf#URI = nullURI)(implicit graph: Rdf#Graph): RawDataForForm[Rdf#Node] = {

    val classesOfSubject = {
      val classes = getClasses(subject)
      if( classes . isEmpty) List(classs) else classes
    }

    val (propsFromConfig, formConfiguration, tryClassFromConfig) =
      computePropsFromConfig(classesOfSubject, formuri)

    val classesOfSubjectOrFromConfig =
      if (classesOfSubject . isEmpty && formuri != nullURI) {
        println(s">>>> computePropertiesList tryClassFromConfig $tryClassFromConfig")
        List( uriNodeToURI(tryClassFromConfig.getOrElse(nullURI)) )
      } else
        classesOfSubject
    println(s">>> computePropsFromConfig( classOfSubjectOrFromConfig=$classesOfSubjectOrFromConfig) => formConfiguration=$formConfiguration, $propsFromConfig")

    val propsFromSubject = fieldsFromSubject(subject, graph)
 
    val propsFromClasses: List[RawDataForForm[Rdf#Node]] = {
    	fieldsFromClasses(classesOfSubjectOrFromConfig, subject, editable, graph)
    }

    val propertiesList0 = (
        propsFromConfig ++
        propsFromSubject ++
        propsFromClasses. map { pp => pp.propertiesList } . flatten
    ).distinct
    val propertiesList = addRDFSLabelComment(propertiesList0)
    val reversePropertiesList = reversePropertiesListFromFormConfiguration(formConfiguration)

//    def makeRawDataForForm(propertiesGroups: collection.Map[Rdf#Node, RawDataForForm[Rdf#Node]]) =
//      RawDataForForm[Rdf#Node](propertiesList, classOfSubjectOrFromConfig, subject, editable,
//        formuri match { case "" => None; case uri => Some(URI(uri)) },
//        reversePropertiesList,
//        propertiesGroups = propertiesGroups)
    def makeRawDataForForm(rawDataForFormList: List[RawDataForForm[Rdf#Node]]): RawDataForForm[Rdf#Node] = {
      val propertiesGroupsList = for( rawDataForForm <- rawDataForFormList ) yield {
        rawDataForForm.propertiesGroups
      }   
      val propertiesGroupsMap = propertiesGroupsList . flatten . toMap
      RawDataForForm[Rdf#Node](propertiesList, classesOfSubjectOrFromConfig.head, subject, editable,
        formuri match { case "" => None; case uri => Some(URI(uri)) },
        reversePropertiesList,
        propertiesGroups = propertiesGroupsMap)
    }

    val rawDataFromSpecif = RawDataForForm[Rdf#Node](
      propsFromConfig, classesOfSubjectOrFromConfig.head, subject, editable)

    return makeRawDataForForm(
//      propsFromClasses.propertiesGroups
      propsFromClasses
//        + (formConfiguration -> rawDataFromSpecif)
        )
  }

  /**
   * look for Properties list from form spec in given URI or else in TDB from given class URI
   *  @return (propertiesList, formConfiguration, tryClass)
   */
  private def computePropsFromConfig(classes: List[Rdf#Node],
                                     formuri: String)(implicit graph: Rdf#Graph): (Seq[Rdf#URI], Rdf#Node, Try[Rdf#Node]) = {
		// TODO loop on classes
    val classs = classes.head

    if (formuri == "") {
      val (propertiesList, formConfiguration) = lookPropertiesListInConfiguration(classs)
      println(s"computePropsFromConfig: $propertiesList")
      (propertiesList, formConfiguration, Success(classs))
    } else {
      val (propertiesList, formConfiguration, tryGraph) = lookPropertiesListFromDatabaseOrDownload(formuri)
      val tryClass = tryGraph.map { gr =>
        lookClassInFormSpec(URI(formuri), gr)
      }
      (propertiesList, formConfiguration, tryClass)
    }
  }
  private def classFromSubject(subject: Rdf#Node)(implicit graph: Rdf#Graph) = {
    getHeadOrElse(subject, rdf.typ)
  }
}
