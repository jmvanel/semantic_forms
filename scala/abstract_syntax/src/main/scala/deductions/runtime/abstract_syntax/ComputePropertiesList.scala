package deductions.runtime.abstract_syntax

import org.w3.banana.RDF

import scala.util.{Success, Try}

/** Step 1 of form generation: compute properties List from Config, Subject, Class (in that order) */
trait ComputePropertiesList[Rdf <: RDF, DATASET] {
  self: FormSyntaxFactory[Rdf, DATASET] =>
  import ops._

//  object NullRawDataForForm extends RawDataForForm[Rdf#Node, Rdf#URI](Seq(), nullURI, nullURI )

  /**
   * create Raw Data For Form (incomplete FormSyntax) from an instance (subject) URI,
   * and possibly a Form Specification URI if URI is not <> ;
   * ( see form_specs/foaf.form.ttl for an example of form Specification);
   *
   * it merges given properties from Specification, with properties from Subject
   * and from Class (in this order).
   * 
   * The class is either:
   * - given as argument
   * - inferred from subject
   * - inferred from form specification
   *
   * @return a FormSyntax data structure
   */
  protected def computePropertiesList(subject: Rdf#Node,
                                      editable: Boolean = false,
                                      formuri: String,
                                      classs: Rdf#URI = nullURI)(implicit graph: Rdf#Graph):
                                      FormSyntax = {

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
 
    val propsFromClasses: List[FormSyntax] = {
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

    def makeformSyntax(formSyntaxList: List[FormSyntax]): FormSyntax = {
      logger.debug(s"""makeformSyntax: formSyntaxList size ${formSyntaxList.size}
        ${formSyntaxList.mkString("\n")}""")
      val propertiesGroupsList = for (formSyntax <- formSyntaxList) yield {
        formSyntax.propertiesGroupMap
      }
      val propertiesGroupsMap = propertiesGroupsList.flatten.toMap
      logger.debug(s"""makeformSyntax: size ${propertiesGroupsMap.size}
        ${propertiesGroupsMap.keySet}""")

      FormSyntax(
        subject,
        Seq(),
        makeEntries(propertiesList),
        classesOfSubjectOrFromConfig,
        editable = editable,
        formURI = formuri match {
          case ""  => Some(formConfiguration);
          case uri => Some(URI(uri))
        },
        reversePropertiesList = reversePropertiesList,
        propertiesGroupMap = propertiesGroupsMap)
    }



    val globalDataForForm = makeformSyntax(propsFromClasses)

    /* formSyntax from Form Specification */
    val formSyntaxFromSpecif: FormSyntax =
      if (formConfiguration != nullURI)
        FormSyntax(
          subject,
          Seq(),
          makeEntries(propsFromConfig),
          Seq(formConfiguration), // TODO : this argument is for class URI's not form URI !!??
          editable = editable)
      else NullFormSyntax
    logger.debug(s"computePropertiesList formSyntaxFromSpecif $formSyntaxFromSpecif")

    /* add Property Group for the default form */
    def prependPropertyGroup(globalDataForForm: FormSyntax, key: Rdf#Node,
                             addedDataForForm: FormSyntax) =
      globalDataForForm.copy(
        propertiesGroupMap =
          globalDataForForm.propertiesGroupMap +
            (key -> addedDataForForm))
            
    return prependPropertyGroup(globalDataForForm, Literal("Short form"), formSyntaxFromSpecif)
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
