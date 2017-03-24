package deductions.runtime.abstract_syntax

import scala.util.Success
import scala.util.Try

import org.w3.banana.RDF

/** intermediary data for form generation:  properties' List, etc */
case class RawDataForForm[Node](
    propertiesList: Seq[Node],
    classs: Node,
    subject: Node,
    editable: Boolean = false,
    formURI: Option[Node] = None,
    reversePropertiesList: Seq[Node] = Seq(),
    /* properties Groups come from multiple super-classes */
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

  object NullRawDataForForm extends RawDataForForm[Rdf#Node](Seq(), nullURI, nullURI )

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
      if (classesOfSubject . isEmpty && formuri != "") {
        println(s"computePropertiesList tryClassFromConfig $tryClassFromConfig")
        List( uriNodeToURI(tryClassFromConfig.getOrElse(nullURI)) )
      } else
        classesOfSubject

      logger.debug(
        s""">>> computePropsFromConfig( classOfSubjectOrFromConfig=$classesOfSubjectOrFromConfig) =>
               formConfiguration=<$formConfiguration>, props From Config $propsFromConfig""")

    val propsFromSubject = fieldsFromSubject(subject, graph)
 
    val propsFromClasses: List[RawDataForForm[Rdf#Node]] = {
    	fieldsFromClasses(classesOfSubjectOrFromConfig, subject, editable, graph)
    }

    val  propsFromClasses2 =
      if( propsFromConfig . isEmpty )
      propsFromClasses. map { pp => pp.propertiesList } . flatten
      else
        Seq()
        
    val propertiesListAllItems = (
        propsFromConfig ++
        propsFromSubject ++
        propsFromClasses2
    ).distinct

    val propertiesList =
      if (propsFromConfig.isEmpty)
        addRDFSLabelComment(propertiesListAllItems)
      else
        propertiesListAllItems
  
    val reversePropertiesList = reversePropertiesListFromFormConfiguration(formConfiguration)


    def makeRawDataForForm(rawDataForFormList: List[RawDataForForm[Rdf#Node]]): RawDataForForm[Rdf#Node] = {
      logger.debug(s"""makeRawDataForForm: rawDataForFormList size ${rawDataForFormList.size}
        ${rawDataForFormList.mkString("\n")}""")
      val propertiesGroupsList = for (rawDataForForm <- rawDataForFormList) yield {
        rawDataForForm.propertiesGroups
      }
      val propertiesGroupsMap = propertiesGroupsList.flatten.toMap
      logger.debug(s"""makeRawDataForForm: size ${propertiesGroupsMap.size}
        ${propertiesGroupsMap.keySet}""")

      RawDataForForm[Rdf#Node](
        propertiesList, classesOfSubjectOrFromConfig.head, subject, editable,
        formuri match { case "" => Some(formConfiguration); case uri => Some(URI(uri)) },
        reversePropertiesList,
        propertiesGroups = propertiesGroupsMap)
    }

    /* local function to mix:
     *  - stuff from the context: propertiesList, classe sOf Subject Or Formm Specif,
     *    subject, editable, form URI */
    def prependPropertyGroup(globalDataForForm: RawDataForForm[Rdf#Node], key: Rdf#Node,
                             addedDataForForm: RawDataForForm[Rdf#Node]) =
      globalDataForForm.copy(
        propertiesGroups =
          globalDataForForm.propertiesGroups +
            (key -> addedDataForForm))

    val globalDataForForm = makeRawDataForForm(propsFromClasses)

    /* RawDataForForm from Form Specification */
    val rawDataFromSpecif: RawDataForForm[Rdf#Node] = if (formConfiguration != nullURI)
      RawDataForForm[Rdf#Node](
        propsFromConfig,
        formConfiguration,
        subject, editable)
    else NullRawDataForForm
    logger.debug(s"computePropertiesList rawDataFromSpecif $rawDataFromSpecif")

    return prependPropertyGroup(globalDataForForm, Literal("Short form"), rawDataFromSpecif)
//    makeRawDataForForm( rawDataFromSpecif ++ propsFromClasses )
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
      logger.debug( s"computePropsFromConfig: $propertiesList")
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
