/* copyright the Déductions Project
under GNU Lesser General Public License
http://www.gnu.org/licenses/lgpl.html
$Id$
 */
package deductions.runtime.abstract_syntax

import scala.collection.mutable
import scala.language.existentials
import scala.language.postfixOps

import org.w3.banana.RDF
import org.w3.banana.RDFPrefix
import org.w3.banana.RDFSPrefix
import org.w3.banana.XSDPrefix

import deductions.runtime.services.Configuration
import deductions.runtime.utils.RDFHelpers
import deductions.runtime.utils.RDFPrefixes
import deductions.runtime.utils.Timer

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
   	with FormConfigurationFactory[Rdf, DATASET]
   	with ComputePropertiesList[Rdf, DATASET]
    with FormConfigurationReverseProperties[Rdf, DATASET]
    with RDFListInference[Rdf, DATASET]
    with ThumbnailInference[Rdf, DATASET]
    with FormSyntaxFromSPARQL[Rdf, DATASET]
    with RDFPrefixes[Rdf]
    with UniqueFieldID[Rdf]
    with Timer {

  val config: Configuration
  import config._
  
  val defaults: FormDefaults = FormModule.formDefaults
  
  import ops._

  val literalInitialValue = ""
  private val rdf_type = RDFPrefix[Rdf].typ

  override def makeURI(n: Rdf#Node): Rdf#URI = URI(foldNode(n)(
    fromUri(_), fromBNode(_), fromLiteral(_)._1))

//  val rdfs = RDFSPrefix[Rdf]
  override lazy val rdf = RDFPrefix[Rdf]
  
  
  /** create Form abstract syntax from an instance (subject) URI;
   *  the Form Specification is inferred from the class of instance;
   *  TODO : add lang argument
   *  NO transaction, should be called within a transaction */
  def createForm(subject: Rdf#Node,
    editable: Boolean = false,
    formGroup: Rdf#URI = nullURI, formuri: String="")
    (implicit graph: Rdf#Graph, lang: String="en" )
  : FormSyntax = {

    val step1 = computePropertiesList(subject, editable, formuri)
    createFormDetailed2( step1, formGroup )
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
   * TODO: propertiesList argument IS NOT USED !!!!
   *
   * used directly for creating an empty Form from a class URI,
   * and indirectly for other cases;
   * NO transaction, should be called within a transaction
   */
  def createFormDetailed(subject: Rdf#Node,
    propertiesList: Iterable[Rdf#Node],
    classs: Rdf#URI,
    formMode: FormMode,
    formGroup: Rdf#URI = nullURI,
    formConfig: Rdf#Node = URI(""))
    (implicit graph: Rdf#Graph) : FormSyntax = {

		val step1 = computePropertiesList(subject, formMode.editable, fromUri(uriNodeToURI(formConfig)), classs )
    createFormDetailed2( step1, formGroup )
  }
  
  /** */
  private def createFormDetailed2(
		  step1: RawDataForForm[Rdf#Node],
		  formGroup: Rdf#URI = nullURI)
    (implicit graph: Rdf#Graph,  lang: String="")
  : FormSyntax = {

    val formConfig = step1.formURI
    logger.info(
//println(
    s">>>> createFormDetailed2 formConfig <$formConfig> lang $lang")
    
    val valuesFromFormGroup = possibleValuesFromFormGroup(formGroup: Rdf#URI, graph)

    def makeEntriesFromRawDataForForm(step1: RawDataForForm[Rdf#Node]): Seq[Entry] = {
      val subject = step1.subject
      val props = step1.propertiesList
      val classs = step1.classs
      val formMode: FormMode = if (step1.editable) EditionMode else DisplayMode

      logger.debug(s"createForm subject <$subject>, props $props")
      logger.debug(
          s"""FormSyntaxFactory: language: "$lang""" )

      val entries = for (
        prop <- props if prop != displayLabelPred
      ) yield {
        logger.debug(s"makeEntriesFromRawDataForForm subject $subject, prop $prop")
          time(s"makeEntriesForSubject(${prop})",
          makeEntriesForSubject(subject, prop, formMode))
      }
      val fields = entries.flatten
      val fields2 = addTypeTriple(subject, classs, fields)
      addInverseTriples(fields2, step1)
    }
    
    //// compute Form Syntax ////

    val fieldsCompleteList = makeEntriesFromRawDataForForm(step1)
    val subject = step1.subject
    val classs = step1.classs

    // set a FormSyntax.title for each group in propertiesGroups
    val entriesFromPropertiesGroups = for( (node, rawDataForForm ) <- step1.propertiesGroups ) yield
    	node -> makeEntriesFromRawDataForForm(rawDataForForm)
    val propertiesGroups = for( (n, m) <- entriesFromPropertiesGroups ) yield {
      FormSyntax(n, m, title=instanceLabel(n, allNamedGraph, lang))
    }
    val formSyntax = FormSyntax(subject, fieldsCompleteList, classs, propertiesGroups=propertiesGroups.toSeq,
        thumbnail = getURIimage(subject),
        title = instanceLabel( subject, allNamedGraph, lang ),
        formURI = step1.formURI,
        formLabel= step1.formURI match {
          case None => ""
          case Some(uri) => instanceLabel( uri, allNamedGraph, lang )
        } )
    
    if( step1.editable ) addAllPossibleValues(formSyntax, valuesFromFormGroup)
    logger.debug(s"createFormDetailed2: createForm " + this)
    
    val res = time(s"createFormDetailed2: updateFormFromConfig(formConfig=$formConfig)",
      updateFormFromConfig(formSyntax, formConfig))
    logger.debug(s"createFormDetailed2: createForm 2 " + this)
    res
  }

  protected def addInverseTriples(fields2: Seq[Entry],
      step1: RawDataForForm[Rdf#Node]): Seq[Entry]

  /**
   * update given Form,
   * looking up for field Configurations within given RDF graph
   * eg in :
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
      } else if (rangesSize == 0) {
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

  /** non recursive update */
  private def updateOneFormFromConfig(formSyntax: FormSyntax, formConfig: Rdf#Node)(implicit graph: Rdf#Graph) = {
    for (field <- formSyntax.fields) {
      val fieldSpecs = lookFieldSpecInConfiguration(field.property)
      if (!fieldSpecs.isEmpty)
        fieldSpecs.map {
          fieldSpec =>
            val triples = find(graph, fieldSpec.subject, ANY, ANY).toSeq
            for (t <- triples)
              field.addTriple(t.subject, t.predicate, t.objectt)
            // TODO each feature should be in a different file
            def replace[T](s: Seq[T], occurence: T, replacement: T): Seq[T] =
              s.map { i => if (i == occurence) replacement else i }
            for (t <- triples) {
              if (t.predicate == formPrefix("widgetClass")
                && t.objectt == formPrefix("DBPediaLookup")) {
                val rep = field.asResource
                rep.widgetType = DBPediaLookup
                formSyntax.fields = replace(formSyntax.fields, field, rep)
              }
            }
        }
    }
    val triples = find(graph, formConfig, ANY, ANY).toSeq
    // TODO try the Object - semantic mapping of Banana-RDF
    val uriToCardinalities = Map[Rdf#Node, Cardinality] {
      formPrefix("zeroOrMore") -> zeroOrMore;
      formPrefix("oneOrMore") -> oneOrMore;
      formPrefix("zeroOrOne") -> zeroOrOne;
      formPrefix("exactlyOne") -> exactlyOne
    }
    for (t <- triples) {
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
  private def addTypeTriple(subject: Rdf#Node, classs: Rdf#Node, // URI,
    fields: Iterable[Entry])
    (implicit graph: Rdf#Graph)
  : Seq[Entry] = {
    val alreadyInDatabase = ! find(graph, subject, rdf.typ, ANY).isEmpty
    if ( // defaults.displayRdfType ||
    !alreadyInDatabase) {
      val classFormEntry = new ResourceEntry(
        // TODO not I18N:
        "type", "class",
        rdf.typ, ResourceValidator(Set(owl.Class)), classs,
        alreadyInDatabase = alreadyInDatabase)
      (fields ++ Seq(classFormEntry)).toSeq
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
//      valuesFromFormGroup: Seq[(Rdf#Node, Rdf#Node)]
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
//    valuesFromFormGroup: Seq[(Rdf#Node, Rdf#Node)] // formGroup: Rdf#URI
    )(implicit graph: Rdf#Graph, lang:String): Entry = {

    val xsdPrefix = XSDPrefix[Rdf].prefixIri
    val rdf = RDFPrefix[Rdf]
    val rdfs = RDFSPrefix[Rdf]

    val precomputProp = PrecomputationsFromProperty(prop)
    import precomputProp.{ prop => _, _ }
    
    def rdfListEntry = makeRDFListEntry(label, comment, prop, objet0, subject = subject)

    val nullLiteral = Literal("")
    val nullBNode = BNode("")
    val chooseRDFNodeType =
        rangeClasses match {
      case _ if objet0.isLiteral => nullLiteral
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

      case _                                    => nullLiteral
    }

    val objet =
      if (objet0 == nullURI)
        chooseRDFNodeType
      else
        objet0
    
//    println(s""">>>> makeEntryFromTriple: prop $prop objet "$objet" ${objet.getClass()} """)
    val htmlName: String = makeHTMLName( makeTriple(subject, uriNodeToURI(prop), objet) )

    def firstType = firstNodeOrElseNullURI(precomputProp.ranges)

    def literalEntry = {
      val value = getLiteralNodeOrElse(objet, literalInitialValue)
      // TODO match graph pattern for interval datatype ; see issue #17
      // case t if t == ("http://www.bizinnov.com/ontologies/quest.owl.ttl#interval-1-5") =>
      new LiteralEntry(label, comment, prop, DatatypeValidator(ranges),
        value,
        subject=subject,
        subjectLabel = instanceLabel(subject, graph, lang),
        type_ = firstType,
        lang = getLang(objet).toString(),
        valueLabel = nodeToString(value),
        htmlName=htmlName)
    }

    val NullResourceEntry = new ResourceEntry("", "", nullURI, ResourceValidator(Set()))
    def resourceEntry = {
      if (showRDFtype || prop != rdf.typ)
        time(s"""resourceEntry objet "$objet" """,
          foldNode(objet)(
            objet => {
              new ResourceEntry(label, comment, prop, ResourceValidator(ranges), objet,
                subject=subject,
                alreadyInDatabase = true,
                valueLabel = instanceLabel(objet, graph, lang),
                subjectLabel = instanceLabel(subject, graph, lang),
                type_ = firstType,
                isImage = isImageTriple(subject, prop, objet, firstType),
                thumbnail = getURIimage(objet),
                htmlName=htmlName
                )
            },
            objet => makeBN(label, comment, prop, ResourceValidator(ranges), objet,
              typ = firstType),
            _ => literalEntry))
      else NullResourceEntry
    }

    def makeBN(label: String, comment: String,
               property: ObjectProperty, validator: ResourceValidator,
               value: Rdf#BNode,
               typ: Rdf#Node = nullURI) = {
      new BlankNodeEntry(label, comment, property, validator, value,
    		subject=subject,
    		subjectLabel = instanceLabel(subject, graph, lang),
        type_ = typ, valueLabel = instanceLabel(value, graph, lang),
        htmlName=htmlName) {
        override def getId: String = nodeToString(value)
      }
    }

    chooseRDFNodeType match {
      case `nullURI` => resourceEntry
      case `nullLiteral` => literalEntry
      case `nullBNode` => resourceEntry // ??????????????? rather makeBN() ???
      case rdf.List => rdfListEntry.get
      case _       => literalEntry
    }
  }

  private case class PrecomputationsFromProperty(prop: Rdf#Node)(implicit graph: Rdf#Graph, lang:String) {
    val label: String = getLiteralInPreferedLanguageFromSubjectAndPredicate(prop, rdfs.label, terminalPart(prop))
    val comment: String = getLiteralInPreferedLanguageFromSubjectAndPredicate(prop, rdfs.comment, "")
    val propClasses: Set[Rdf#Node] = objectsQuery(prop, rdf_type)
    val ranges: Set[Rdf#Node] = getRDFSranges(prop)
    val rangeClasses: Set[Rdf#Node] = objectsQueries(ranges, rdf_type)
  }
      
  private def firstNodeOrElseNullURI(set: Set[Rdf#Node]): Rdf#Node = set.headOption.getOrElse(nullURI)
}
