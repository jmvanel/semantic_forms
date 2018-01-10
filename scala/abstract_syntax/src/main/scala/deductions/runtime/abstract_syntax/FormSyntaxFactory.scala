/* copyright the Déductions Project
under GNU Lesser General Public License
http://www.gnu.org/licenses/lgpl.html
$Id$
 */
package deductions.runtime.abstract_syntax

import deductions.runtime.utils.{Configuration, RDFHelpers, RDFPrefixes, Timer}
import org.w3.banana.{PointedGraph, RDF, RDFPrefix, RDFSPrefix, XSDPrefix}

import scala.collection.mutable
import scala.language.{existentials, postfixOps}
import deductions.runtime.core._
import scala.util.Success
import scala.util.Failure

import scalaz._
import Scalaz._

/** one of EditionMode, DisplayMode, CreationMode */
abstract sealed class FormMode { val editable = true }
object FormMode{ def apply( editable:Boolean = true) = if(editable) EditionMode else DisplayMode }
object EditionMode extends FormMode { override def toString() = "EditionMode" }
object DisplayMode extends FormMode {
  override def toString() = "DisplayMode"
  override val editable = false }
object CreationMode extends FormMode { override def toString() = "CreationMode" }

/** simple data class to hold an URI with its computed label (rdfs:label, foaf;name, etc) */
class ResourceWithLabel[Rdf <: RDF](val resource: Rdf#Node, val label: Rdf#Node) {
  def this(couple: (Rdf#Node, Rdf#Node)) =
    this(couple._1, couple._2)
  override def toString() = "" + resource + " : " + label
  // def apply[Rdf <: RDF](couple: (Rdf#Node, Rdf#Node) ) = new ResourceWithLabel(couple)
}

/** store in memory the "Possible Values" for each RDF class;
 * by "Possible Values" one means the objets URI's whose class match the rdfs:range
 * of a property in the form. */
trait PossibleValues[Rdf <: RDF] {
  /** Correspondence between resource for a class and
   *  instances of that class with their human readable label */
  private var possibleValues = Map[Rdf#Node, Seq[ResourceWithLabel[Rdf]]]()
  /** check if possible Values in this (class) URI have already been computed */
  def isDefined(uri: Rdf#Node): Boolean = possibleValues.contains(uri)
  /** record given possible Values (ResourceWithLabel) for this class URI */
  def addPossibleValues(uri: Rdf#Node, values: Seq[ResourceWithLabel[Rdf]]) = {
    possibleValues = possibleValues + (uri -> values)
    resourcesWithLabel2Tuples(values)
  }
  def getPossibleValues( uri: Rdf#Node): Seq[ResourceWithLabel[Rdf]] = { possibleValues.getOrElse(uri, Seq()) }
  def getPossibleValuesAsTuple(uri: Rdf#Node): Seq[(Rdf#Node, Rdf#Node)] = {
    resourcesWithLabel2Tuples( getPossibleValues(uri) )
  }
  
  def resourcesWithLabel2Tuples(values: Seq[ResourceWithLabel[Rdf]]) =
    values.map { r => (r.resource, r.label) }
  
  def tuples2ResourcesWithLabel(tuples: Seq[(Rdf#Node, Rdf#Node)]) = {
	  tuples.map { (couple: (Rdf#Node, Rdf#Node)) =>
	  new ResourceWithLabel(couple._1, couple._2)
	  }
  }
}


/**
 * Factory for an abstract Form Syntax;
 * the main class here;
 * NON transactional
 */
trait FormSyntaxFactory[Rdf <: RDF, DATASET]
    extends FormModule[Rdf#Node, Rdf#URI]
    with RDFHelpers[Rdf]
    with FieldsInference[Rdf, DATASET]
    with RangeInference[Rdf, DATASET]
    with PreferredLanguageLiteral[Rdf]
    with PossibleValues[Rdf]
   	with FormSpecificationFactory[Rdf, DATASET]
   	with ComputePropertiesList[Rdf, DATASET]
    with FormConfigurationReverseProperties[Rdf, DATASET]
    with RDFListInference[Rdf, DATASET]
    with ThumbnailInference[Rdf, DATASET]
    with FormSyntaxFromSPARQL[Rdf, DATASET]
    with RDFPrefixes[Rdf]
    with UniqueFieldID[Rdf]
    //with UserTraceability[Rdf, DATASET]
    with OWLsameAsFormProcessing[Rdf, DATASET]
    with Timer
    with FormGroups[Rdf, DATASET] {

  val config: Configuration
  import config._
  
  val defaults: FormDefaults = FormModule.formDefaults
  
  import ops._
//  object NullRawDataForForm extends RawDataForForm[Rdf#Node, Rdf#URI](Seq(), nullURI, nullURI )
  //val NullRawDataForForm = RawDataForForm[Rdf#Node, Rdf#URI](Seq(), nullURI, nullURI )
  val NullFormSyntax = FormSyntax(nullURI,Seq(),Seq())

  val literalInitialValue = ""
  private val rdf_type = RDFPrefix[Rdf].typ

  override def makeURI(n: Rdf#Node): Rdf#URI = URI(foldNode(n)(
    fromUri(_), fromBNode(_), fromLiteral(_)._1))

  override lazy val rdf = RDFPrefix[Rdf]
  
  
  /** create Form abstract syntax from an instance (subject) URI;
   *  the Form Specification is inferred from the class of instance;
   *  NO transaction, should be called within a transaction */
  private def createForm(subject: Rdf#Node,
    editable: Boolean = false,
    formGroup: Rdf#URI = nullURI, formuri: String="")
    (implicit graph: Rdf#Graph, lang: String="en" )
  : FormSyntax = {

    val step1 = computePropertiesList(subject, editable, formuri)
    val step2 = addOWLsameAs(step1)
    // TODO val step3 = addOWLinverseOf(step2)   
    createFormDetailed2( step2, formGroup )
  }

  /**
   * create Form abstract syntax from an instance (subject) URI;
   *  the Form Specification is inferred from the class of instance;
   *  with transaction, should NOT be called within a transaction
   */
  def createFormTR(subject: Rdf#Node,
                   editable: Boolean = false,
                   formGroup: Rdf#URI = nullURI, formuri: String = "")(implicit graph: Rdf#Graph, lang: String = "en"): FormSyntax = {

    val tryFormSyntax = for (
      step1 <- rdfStore.rw(dataset,
        { computePropertiesList(subject, editable, formuri) });
      step2 <- rdfStore.r(dataset,
        { addOWLsameAs(step1) })
    // TODO val step3 = addOWLinverseOf(step2)
    ) yield {
      rdfStore.rw(dataset,
        { createFormDetailed2(step2, formGroup) })
    }
    tryFormSyntax.flatten match {
      case Success(fs) =>
        logger.debug(s"createFormTR: Success FormSyntax: $fs")
        fs
      case Failure(f) =>
        System.err.println(s"createFormTR: ERROR: $f, getCause ${f.getCause} ${f.getStackTrace().mkString("\n")}")
        FormSyntax(nullURI, Seq())
    }
  }

  private [abstract_syntax] def addRDFSLabelComment(propertiesList: Seq[Rdf#Node]): Seq[Rdf#Node] = {
    if (addRDFS_label_comment &&
      !propertiesList.contains(rdfs.label)) {
      Seq(rdfs.label, rdfs.comment) ++ propertiesList
    } else propertiesList
  }

  /**
   * create Form With Detailed arguments: RDF Properties;
   * For each given property (props)
   *  look at its rdfs:range ?D
   *  see if ?D is a datatype or an OWL or RDFS class;
   * 
   * TODO : add lang argument
   *
   * used directly for creating an empty Form from a class URI,
   * and indirectly for other cases;
   * NO transaction, should be called within a transaction
   */
  def createFormDetailed(subject: Rdf#Node,
    classs: Rdf#URI,
    formMode: FormMode,
    formGroup: Rdf#URI = nullURI,
    formConfig: Rdf#Node = URI(""))
    (implicit graph: Rdf#Graph, lang:String) : FormSyntax = {

		val step1 = computePropertiesList(subject, formMode.editable, fromUri(uriNodeToURI(formConfig)), classs )
    createFormDetailed2( step1, formGroup )
  }
  
  /** */
  private def createFormDetailed2(
		  step1: FormSyntax,
		  formGroup: Rdf#URI = nullURI)
    (implicit graph: Rdf#Graph,  lang: String="")
  : FormSyntax = {

    val formConfig = step1.formURI
    logger.info(
    s">>>> createFormDetailed2 fields size ${step1.fields.size}, formConfig <$formConfig> , lang $lang")
    
    // TODO make it functional #170
    // http://sujitpal.blogspot.fr/2013/06/functional-chain-of-responsibility.html
    // https://www.scala-lang.org/blog/2016/12/07/implicit-function-types.html
    val valuesFromFormGroup = possibleValuesFromFormGroup(formGroup: Rdf#URI, graph)


    //// compute Form Syntax ////

    // TODO make it functional #170
    val fieldsCompleteList: Seq[Entry] = makeEntriesFromFormSyntax(step1)
//    println( s"==== createFormDetailed2: fieldsCompleteList $fieldsCompleteList")
    val subject = step1.subject
    val classs = step1.classs


    val formSyntax0 = FormSyntax(
        subject,
        fieldsCompleteList ++ step1.fields,
//        Seq(),
        classs,
        thumbnail = getURIimage(subject),
        title = makeInstanceLabel( subject, allNamedGraph, lang ),
        formURI = step1.formURI,
        formLabel= step1.formURI match {
          case None => ""
          case Some(uri) => makeInstanceLabel( uri, allNamedGraph, lang )
        } )

    val formSyntax = expandPropertiesGroups(graph, lang)(formSyntax0)

//    check(formSyntax.fields, "formSyntax")

    // TODO make it functional #170
    if( step1.editable ) addAllPossibleValues(formSyntax, valuesFromFormGroup)
    logger.debug(s"createFormDetailed2: createForm " + this)

    // TODO make it functional #170
    val res = time(
      s"createFormDetailed2: updateFormFromConfig(formConfig=$formConfig)",
      updateFormFromConfig(formSyntax, formConfig))
    logger.debug(s"createFormDetailed2: createForm 2 " + this)
//        check(formSyntax.fields, "res")
    res
  }

  /** the heart of the algo: create the form entries from properties List */
  protected def makeEntriesFromFormSyntax(step1: FormSyntax)
  (implicit graph: Rdf#Graph,  lang: String="")
  : Seq[Entry] = {
      val subject = step1.subject
      val props = step1.propertiesList
      val classses = step1.classs
      val formMode: FormMode = if (step1.editable) EditionMode else DisplayMode

//      println( s"""==== makeEntriesFromFormSyntax: step1 $step1
//          entriesList ${step1.entriesList.mkString("\n")}
//      """)

      logger.debug(
          s"makeEntriesFromformSyntax subject <$subject>, classs <$classses>, props $props")

      val entries = for (
        prop <- props if prop != displayLabelPred
      ) yield {
        logger.debug(s"makeEntriesFromformSyntax subject $subject, prop $prop")
          time(s"makeEntriesForSubject(${prop})",
          makeEntriesForSubject(subject, prop, formMode))
      }
      val fields = entries.flatten
//    	println( s"""==== makeEntriesFromFormSyntax: fields $fields""" )

      val fields2 = addTypeTriples(subject, classses, fields)
      addInverseTriples(fields2, step1)
    }

  protected def addInverseTriples(fields2: Seq[Entry],
      step1: FormSyntax): Seq[Entry]

  /**
   * update given Form,
   * looking up for field Specifications within given RDF graph, eg in :
   *  <pre>
   *  &lt;topic_interest> :fieldAppliesToForm &lt;personForm> ;
   *   :fieldAppliesToProperty foaf:topic_interest ;
   *   :widgetClass form:DBPediaLookup .
   *  <pre>
   */
  private def updateFormFromConfig(formSyntax: FormSyntax, formConfigOption: Option[Rdf#Node])(implicit graph: Rdf#Graph): FormSyntax = {
    formConfigOption.map {
      formConfig =>
        updateOneFormFromConfig(formSyntax, formConfig)
        // shallow recursion
        for ( fs <- formSyntax.propertiesGroups ) updateOneFormFromConfig(fs, formConfig)
    }
    formSyntax
  }

  private def getRDFSranges(prop: Rdf#Node)(implicit graph: Rdf#Graph): Set[Rdf#Node] = {
    val ranges = objectsQuery(prop, rdfs.range)
    val rangesSize = ranges.size
    logger.debug(
      if (rangesSize > 1) {
        s"""WARNING: ranges $ranges for property $prop are multiple;
            taking first if not owl:Thing
            """
      } else if (rangesSize === 0) {
        s"""WARNING: There is no range for property $prop
            """
      } else "")

    if (rangesSize > 1) {
      val owlThing = prefixesMap2("owl")("Thing")
      if (ranges.contains(owlThing)) {
        logger.warn(
          s"""WARNING: ranges $ranges for property <$prop> contain owl:Thing;
               removing owl:Thing, and take first remaining: <${(ranges - owlThing).head}>""")
      }
      ranges - owlThing
    } else ranges
  }

  /** non recursive update of given `formSyntax` from given `formSpecif`;
   * see form_specs/foaf.form.ttl for an example of form specification */
  private def updateOneFormFromConfig(formSyntax: FormSyntax, formSpecif: Rdf#Node)(implicit graph: Rdf#Graph)
  : Unit // TODO #170 FormSyntax
  = {

	  // TODO try the Object - semantic mapping of Banana-RDF
    val uriToCardinalities = Map[Rdf#Node, Cardinality] {
      formPrefix("zeroOrMore") -> zeroOrMore;
      formPrefix("oneOrMore") -> oneOrMore;
      formPrefix("zeroOrOne") -> zeroOrOne;
      formPrefix("exactlyOne") -> exactlyOne
    }
    val triplesInFormConfig = find(graph, formSpecif, ANY, ANY).toSeq

    for (field <- formSyntax.fields) {
      val fieldSpecs = lookFieldSpecInConfiguration(field.property)
      if (!fieldSpecs.isEmpty)
        fieldSpecs.map {
          fieldSpec =>
//            println( s">>>> fieldSpec $fieldSpec" )
            val specTriples = find(graph, fieldSpec.subject, ANY, ANY).toSeq
            for (t <- specTriples)
              field.addTriple(t.subject, t.predicate, t.objectt)

            def replace[T](s: Seq[T], occurence: T, replacement: T): Seq[T] =
              s.map { i => if (i == occurence) replacement else i }

            // TODO each feature (Lookup, cardinality) should be in a different file

            for (specTriple <- specTriples) {

              //// DBPedia Lookup ////

              //            	println( s">>>> updateOneFormFromConfig triple $t" )
              if (specTriple.predicate == formPrefix("widgetClass")
                && specTriple.objectt == formPrefix("DBPediaLookup")) {
//                val field2 = field.asResource.copy(widgetType = DBPediaLookup)
                val field2 = field.copyEntry(widgetType = DBPediaLookup)
                formSyntax.fields = replace(formSyntax.fields, field, field2)
//                println(
                logger.debug(s"updateOneFormFromConfig: Lookup: $field -> $field2")
              }

              //// cardinality ////

              if (specTriple.predicate == formPrefix("cardinality")) {
            	  /* Decode such RDF:
            	  forms:givenName--personPerson
	            	  :fieldAppliesToForm forms:personForm ;
	            	  :fieldAppliesToProperty foaf:givenName ;
                    	  :cardinality :exactlyOne .
            	   */
                val formSpecGraph = PointedGraph(specTriple.subject, allNamedGraph) // Graph(triplesInFormConfig))
                for (
                  card <- (formSpecGraph / formPrefix("cardinality")).takeOnePointedGraph;
                  cardinality = uriToCardinalities(card.pointer);
                  prop <- (formSpecGraph / formPrefix("fieldAppliesToProperty")).takeOnePointedGraph;
                  property = prop.pointer;
                  formPointedGraph <- (formSpecGraph / formPrefix("fieldAppliesToForm")).takeOnePointedGraph;
                  form = formPointedGraph.pointer
                ) {
                  // TODO asResource: wrong !!!!!!!!!! => implement copy for an Entry
//                  val field2 = field.asResource.copy(cardinality = cardinality)
                  val field2 = field.copyEntry(cardinality = cardinality)                 
                  formSyntax.fields = replace(formSyntax.fields, field, field2)
//                  println(
                  logger.debug(s"updateOneFormFromConfig: cardinality: prop $prop: $cardinality, $field -> $field2")
                }
              }
            }
        }
    }

    for (t <- triplesInFormConfig) {
      logger.debug("updateFormForClass formConfig " + t)
      if (t.predicate == formPrefix("defaultCardinality")) {
        formSyntax.defaults.defaultCardinality = uriToCardinalities.getOrElse(t.objectt, zeroOrOne)
      }
    }
  }

  /**
   * add Triple: <subject> rdf:type <classs>
   *  @return augmented fields argument
   */
  private def addTypeTriples(subject: Rdf#Node, classses: Seq[Rdf#Node],
                             fields: Iterable[Entry])(implicit graph: Rdf#Graph): Seq[Entry] = {
    val alreadyInDatabase = !find(graph, subject, rdf.typ, ANY).isEmpty
    if ( // defaults.displayRdfType ||
    !alreadyInDatabase
      && !(subject == nullURI)) {
      val classFormEntry =
        for (classs <- classses) yield {
          new ResourceEntry(
            "type", "class", // TODO not I18N
            rdf.typ, ResourceValidator(Set(owl.Class)), classs,
            alreadyInDatabase = alreadyInDatabase,
            htmlName = makeHTMLName(makeTriple(subject, rdf.typ, nullURI)))
        }
      (fields ++ classFormEntry).toSeq
    } else fields.toSeq
  }

  /** make form Entries (possibly several lines in form) for given subject and property,
   * thus taking in account multi-valued properties;
   * try to get rdfs:label, comment, rdf:type,
   * or else display terminal Part of URI as label;
   */
  private def makeEntriesForSubject(
      subject: Rdf#Node, prop: Rdf#Node,
      formMode: FormMode
      )
	  (implicit graph: Rdf#Graph, lang:String)
  : Seq[Entry] = {
    logger.debug( s"makeEntriesForSubject subject <$subject>, prop <$prop> lang $lang")

    val objects = objectsQuery(subject, uriNodeToURI(prop) ); logger.debug(s"makeEntriesForSubject subject <$subject>, objects: $objects")
    val result = mutable.ArrayBuffer[Entry]()
    for (obj <- objects)
      result += makeEntryFromTriple(subject, prop, obj, formMode)

    if (objects isEmpty) result += makeEntryFromTriple(subject, prop, nullURI, formMode)

    logger.debug("result: Entry's " + result)
    result
  }

  /** make Entry (a single line in form) From Triple */
  protected def makeEntryFromTriple(
    subject: Rdf#Node,
    prop: Rdf#Node,
    objet0: Rdf#Node,
    formMode: FormMode
    )(implicit graph: Rdf#Graph, lang:String): Entry = {

    val xsdPrefix = XSDPrefix[Rdf].prefixIri
    val rdfs = RDFSPrefix[Rdf]

    val precomputProp = PrecomputationsFromProperty(prop)
    import precomputProp.{prop => _, _}
    
    def rdfListEntry = makeRDFListEntry(label, comment, prop, objet0, subject = subject)

    val nullLiteral = Literal("")
    val nullBNode = BNode("")

    val chooseRDFNodeType: Rdf#Node =
      rangeClasses match {
        // give priority to actual value (triple object) over property
        case _ if objet0.isLiteral &&
          !propClasses.contains(owl.ObjectProperty) => nullLiteral

        case _ if rangeClasses.exists { c => c.toString startsWith (xsdPrefix) } => nullLiteral
        case _ if rangeClasses.contains(rdfs.Literal) => nullLiteral
        case _ if propClasses.contains(owl.DatatypeProperty) => nullLiteral
        case _ if ranges.contains(rdfs.Literal) => nullLiteral

        case _ if propClasses.contains(owl.ObjectProperty) => nullURI
        case _ if rangeClasses.contains(owl.Class) => nullURI
        case _ if rangeClasses.contains(rdf.Property) => nullURI
        case _ if ranges.contains(owl.Thing) => nullURI
        case _ if isURI(objet0) => nullURI

        case _ if rdfListEntry.isDefined => rdf.List
        case _ if isBN(objet0) => nullBNode
        case _ if objet0.toString.startsWith("_:") => nullBNode

        case _ => nullLiteral
      }

    val objet =
      if (objet0 == nullURI)
        chooseRDFNodeType
      else
        objet0
    
    def htmlName: String = makeHTMLName( makeTriple(subject, uriNodeToURI(prop), objet) )

    def firstType = firstNodeOrElseNullURI(precomputProp.ranges)
    def typesFromRanges = precomputProp.ranges.toSeq

    def literalEntry = {
      val value = getLiteralNodeOrElse(objet, literalInitialValue)
      val widgetType = makeWidgetTypeFromTriple(
        subject, prop, objet0, formMode)
      // TODO match graph pattern for interval datatype ; see issue #17
      // case t if t == ("http://www.bizinnov.com/ontologies/quest.owl.ttl#interval-1-5") =>
      val res = LiteralEntry(label, comment, prop, DatatypeValidator(ranges),
        value,
        subject = subject,
        subjectLabel = makeInstanceLabel(subject, graph, lang),
        type_ = typesFromRanges, //  firstType,
        lang = getLang(objet).toString(),
        valueLabel = nodeToString(value),
        htmlName = htmlName,
        widgetType = widgetType)
      //      println(s">>>> literalEntry ${res.valueLabel} - $res")
      res
    }

    def resourceEntry = {
      ResourceEntry(
        label, comment, prop, ResourceValidator(ranges), objet,
        subject = subject,
        alreadyInDatabase = true,
        valueLabel = makeInstanceLabel(objet, graph, lang),
        subjectLabel = makeInstanceLabel(subject, graph, lang),
        type_ = typesFromRanges,

        // TODO make it functional #170:  modularize in ThumbnailInference, leveraging on addAttributesToXMLElement
        isImage = isImageTriple(subject, prop, objet, firstType),
        thumbnail = getURIimage(objet),

        isClass = prop == rdf.typ,
        htmlName = htmlName)
    }
    /* UNUSED */
    def entryFromObject = {
      if (showRDFtype || prop != rdf.typ) {
        val res = time(s"""resourceEntry objet "$objet" """,
          foldNode(objet)(
            objet => resourceEntry,
            objet => makeBN(label, comment, prop, ResourceValidator(ranges), objet,
              typ = firstType),
            _ => literalEntry))
        res
      } else NullResourceEntry
    }

    def makeBN(label: String, comment: String,
               property: ObjectProperty, validator: ResourceValidator,
               value: Rdf#BNode,
               typ: Rdf#Node = nullURI) = {
      new BlankNodeEntry(label, comment, property, validator, value,
    		subject=subject,
    		subjectLabel = makeInstanceLabel(subject, graph, lang),
        type_ = Seq(typ), valueLabel = makeInstanceLabel(value, graph, lang),
        htmlName=htmlName) {
        override def getId: String = nodeToString(value)
      }
    }

//    println(s">>>> makeEntryFromTriple <$prop>, chooseRDFNodeType '$chooseRDFNodeType' ${chooseRDFNodeType == nullURI}")
    chooseRDFNodeType match {
      case `nullURI` =>
        val re = resourceEntry
//        println(s">>>> makeEntryFromTriple resourceEntry $re")
        re
      case `nullLiteral` => literalEntry
      case `nullBNode` => resourceEntry // ??????????????? rather makeBN() ???
      case rdf.List => rdfListEntry.get
      case _       => literalEntry
    }
  }

  /** make Widget Type From Triple, for literal triple */
  private def makeWidgetTypeFromTriple(
    subject: Rdf#Node,
    prop: Rdf#Node,
    objet0: Rdf#Node,
    formMode: FormMode
    )(implicit graph: Rdf#Graph, lang:String): WidgetType = {
    val isShortString = ! find( graph, prop, form("shortString"),
        Literal("true", xsd.boolean)) . isEmpty
    val res = if( isShortString )
      ShortString
    else Textarea
//    println(s"==== makeWidgetTypeFromTriple: $prop -> $res")
    res
  }

  private case class PrecomputationsFromProperty(prop: Rdf#Node)(implicit graph: Rdf#Graph, lang:String) {
    val label: String = getLiteralInPreferedLanguageFromSubjectAndPredicate(prop, rdfs.label, terminalPart(prop))
    val comment: String = getLiteralInPreferedLanguageFromSubjectAndPredicate(prop, rdfs.comment, "")
    val propClasses: Set[Rdf#Node] = objectsQuery(prop, rdf_type)
    val ranges: Set[Rdf#Node] = getRDFSranges(prop)
    val rangeClasses: Set[Rdf#Node] = objectsQueries(ranges, rdf_type)
  }
      
}
