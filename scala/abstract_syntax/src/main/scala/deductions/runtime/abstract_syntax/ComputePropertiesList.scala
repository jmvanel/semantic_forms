package deductions.runtime.abstract_syntax

import org.w3.banana.RDF

import scala.util.{Success, Try}
import scala.collection.mutable.ArrayBuffer

import scalaz._
import Scalaz._

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

    val (propsFromFormsSpecs, formSpecs, tryClassFromConfig) =
      computePropsFromConfig(classesOfSubject, formuri)

    val classesOfSubjectOrFromConfig =
      if (classesOfSubject . isEmpty && formuri =/= "") {
        logger.info(s"computePropertiesList tryClassFromConfig $tryClassFromConfig")
        List( uriNodeToURI(tryClassFromConfig.getOrElse(nullURI)) )
      } else
        classesOfSubject

      logger.debug(
        s""">>> computePropsFromConfig( classOfSubjectOrFromConfig=$classesOfSubjectOrFromConfig) =>
               formConfiguration=<$formSpecs>, props From Config $propsFromFormsSpecs""")

    val propsFromSubject = fieldsFromSubject(subject, graph)
 
    val formSyntaxesFromClasses: List[FormSyntax] = {
    	fieldsFromClasses(classesOfSubjectOrFromConfig, subject, editable, graph)
    }

    // only add properties that were not in form specs
    val  propsFromClasses2 = {
      val propsFromClasses = formSyntaxesFromClasses. map { pp => pp.propertiesList } . flatten
      propsFromClasses . diff( propsFromFormsSpecs )
    }
        
    val propertiesList = (
        addRDFSLabelComment(propsFromFormsSpecs) ++
      (if (!propsFromSubject.isEmpty)
        form("separator_props_From_Subject") ::
        propsFromSubject.toList
      else Seq()) ++
      (if (!propsFromClasses2.isEmpty)
        form("separator_props_From_Classes") ::
        propsFromClasses2.toList
      else Seq())
    ).distinct
  
    val reversePropertiesList =
      reversePropertiesListFromFormConfiguration(
          formSpecs.head) // TODO <<<<<<<<<<<

    /** make form Syntax from underlying propertiesList;
     *  propertiesGroupsMap is taken from argument */
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
//        Seq(),
        makeEntries(propertiesList),
        classesOfSubjectOrFromConfig,
        editable = editable,
        formURI = formuri match {
          case ""  => Some(formSpecs.head); // TODO
          case uri => Some(URI(uri))
        },
        reversePropertiesList = reversePropertiesList,
        propertiesGroupMap = propertiesGroupsMap)
    }



    val globalDataForForm = makeformSyntax(formSyntaxesFromClasses)

    /* formSyntax from Form Specification */
    val formSyntaxFromSpecif: FormSyntax =
      if (formSpecs != nullURI)
        FormSyntax(
          subject,
//          Seq(),
          makeEntries(propsFromFormsSpecs),
          formSpecs, // TODO : this argument is for class URI's not form URI !!??
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
   *  (several FormSyntax)
   */
  private def computePropsFromConfig(classes: List[Rdf#Node],
                                     formuri: String)
  (implicit graph: Rdf#Graph):
	  (Seq[Rdf#URI], Seq[Rdf#Node], Try[Rdf#Node]) = {

    // loop on classes
    val listPerFormSpecification =
      if (formuri === "") {
        for (classe <- classes) yield {
          val (propertiesList, formConfiguration) = lookPropertiesListInConfiguration(classe)
          //          logger.debug(
          logger.debug(
            s"computePropsFromConfig: class <$classe> : $propertiesList")
          (propertiesList.toSeq, formConfiguration, Success(classe))
        }
      } else {
        val (propertiesList, formSpecification, tryGraph) = lookPropertiesListFromDatabaseOrDownload(formuri)
        val tryClass = tryGraph.map { gr =>
          lookClassInFormSpec(URI(formuri), gr)
        }
          logger.info(
            s"computePropsFromConfig: formuri != <> propertiesList $propertiesList tryClass $tryClass")
        List((propertiesList, formSpecification, tryClass))
      }
    // concatenate all properties List
    val propertiesListAll = ArrayBuffer[Rdf#URI]()
    val formSpecificationAll = ArrayBuffer[Rdf#Node]()
    var tryClassLast: Try[Rdf#Node] = null
    for( formSpecificationTuple <- listPerFormSpecification) yield {
      val (propertiesList, formSpecification, tryClass) = formSpecificationTuple
      // TODO interpose special null property for separation
      propertiesListAll.appendAll(propertiesList)
      formSpecificationAll.append(formSpecification)
      tryClassLast = tryClass
    }
    (propertiesListAll.toSeq, formSpecificationAll.toSeq, tryClassLast)
  }

  private def computePropsFromConfigOLD(classes: List[Rdf#Node],
                                     formuri: String)
  (implicit graph: Rdf#Graph):
	  (Seq[Rdf#URI], Seq[Rdf#Node], Try[Rdf#Node]) = {

    // loop on classes
    val listPerFormSpecification =
      for (classe <- classes) yield {
        if (formuri === "") {
          val (propertiesList, formConfiguration) = lookPropertiesListInConfiguration(classe)
//          logger.debug(
              logger.debug(
              s"computePropsFromConfig: class <$classe> : $propertiesList")
          (propertiesList, formConfiguration, Success(classe))
        } else {
          val (propertiesList, formSpecification, tryGraph) = lookPropertiesListFromDatabaseOrDownload(formuri)
          val tryClass = tryGraph.map { gr =>
            lookClassInFormSpec(URI(formuri), gr)
          }
          (propertiesList, formSpecification, tryClass)
        }
      }
    // concatenate all properties List
    val propertiesListAll = ArrayBuffer[Rdf#URI]()
    val formSpecificationAll = ArrayBuffer[Rdf#Node]()
    var tryClassLast: Try[Rdf#Node] = null
    for( formSpecificationTuple <- listPerFormSpecification) yield {
      val (propertiesList, formSpecification, tryClass) = formSpecificationTuple
      // TODO interpose special null property for separation
      propertiesListAll.appendAll(propertiesList)
      formSpecificationAll.append(formSpecification)
      tryClassLast = tryClass
    }
    (propertiesListAll.toSeq, formSpecificationAll.toSeq, tryClassLast)
  }
    
  private def classFromSubject(subject: Rdf#Node)(implicit graph: Rdf#Graph) = {
    getHeadOrElse(subject, rdf.typ)
  }
}
