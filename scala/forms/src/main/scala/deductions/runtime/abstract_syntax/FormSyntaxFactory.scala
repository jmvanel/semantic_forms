/* copyright the Déductions Project
under GNU Lesser General Public License
http://www.gnu.org/licenses/lgpl.html
$Id$
 */
package deductions.runtime.abstract_syntax

import scala.collection.mutable
import org.apache.log4j.Logger
import org.w3.banana.OWLPrefix
import org.w3.banana.PointedGraph
import org.w3.banana.RDF
import org.w3.banana.RDFDSL
import org.w3.banana.RDFOps
import org.w3.banana.RDFPrefix
import org.w3.banana.RDFSPrefix
import org.w3.banana.URIOps
import org.w3.banana.XSDPrefix
import org.w3.banana.diesel._
import org.w3.banana.Prefix
import org.w3.banana.syntax._
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import org.w3.banana.LocalNameException
import org.w3.banana.RDFStore
import deductions.runtime.jena.RDFStoreObject
import deductions.runtime.utils.RDFHelpers

object FormSyntaxFactory {
  /** vocabulary for form specifications */
  val formVocabPrefix = "http://deductions-software.com/ontologies/forms.owl.ttl#"
}

/**
 * Factory for an abstract Form Syntax;
 * the main class here;
 * NON transactional
 */
class FormSyntaxFactory[Rdf <: RDF](val graph: Rdf#Graph, preferedLanguage: String = "en")(implicit val ops: RDFOps[Rdf],
  val uriOps: URIOps[Rdf])

    extends FormModule[Rdf#Node, Rdf#URI]
    with FieldsInference[Rdf]
    with RangeInference[Rdf] {

  import ops._

  lazy val nullURI = ops.URI("http://null.com#") // TODO better : "" ????????????
  val rdfs = RDFSPrefix[Rdf]

  val owl = OWLPrefix[Rdf]
  val owlThing = owl.prefixIri + "Thing"
  val rdf = RDFPrefix[Rdf]
  import FormSyntaxFactory._
  val formPrefix: Prefix[Rdf] = Prefix("form", formVocabPrefix)
  val gr = graph
  val rdfh = new RDFHelpers[Rdf] { val graph = gr }
  import rdfh._
  println("FormSyntaxFactory: preferedLanguage: " + preferedLanguage)

  /** create Form from an instance (subject) URI */
  def createForm(subject: Rdf#Node,
    editable: Boolean = false): FormSyntax[Rdf#Node, Rdf#URI] = {
    val propsFromSubject = fieldsFromSubject(subject, graph)
    val classs = classFromSubject(subject) // TODO several classes
    val propsFromClass =
      if (editable) {
        fieldsFromClass(classs, graph)
      } else Seq()
    createForm(subject, (propsFromSubject ++ propsFromClass).distinct, classs)
  }

  /**
   * create Form With Given Properties;
   * For each given property (props)
   *  look at its rdfs:range ?D
   *  see if ?D is a datatype or an OWL or RDFS class
   *  ( used for creating an empty Form from a class URI )
   */
  def createForm(subject: Rdf#Node,
    // 
    //    props: Seq[Rdf#URI], classs: Rdf#URI): FormSyntax[Rdf#Node, Rdf#URI] = {
    props: Iterable[Rdf#URI], classs: Rdf#URI): FormSyntax[Rdf#Node, Rdf#URI] = {
    Logger.getRootLogger().info(s"createForm subject $subject, props $props")

    val entries = for (prop <- props) yield {
      Logger.getRootLogger().info(s"createForm subject $subject, prop $prop")
      //      val ranges = extractURIs(objectsQuery(prop, rdfs.range))
      val ranges = nodeSeqToURISet(objectsQuery(prop, rdfs.range))
      val rangesSize = ranges.size
      val mess = if (rangesSize > 1) {
        "WARNING: ranges " + ranges + " for property " + prop + " are multiple."
      } else if (rangesSize == 0) {
        "WARNING: There is no range for property " + prop
      } else ""
      println(mess, System.err)
      makeEntries(subject, prop, ranges)
    }
    val fields = entries.flatMap { s => s }
    val fields2 = addTypeTriple(subject, classs, fields)
    val formSyntax = FormSyntax(subject, fields2, classs)
    updateFormForClass(formSyntax)
  }

  type AbstractForm = FormSyntax[Rdf#Node, Rdf#URI]
  val formConfiguration = new FormConfigurationFactory[Rdf](graph)
  /**
   * update given Form,
   * looking up for Form Configuration within RDF graph in this class
   * e g in :
   *  <pre>
   *  <topic_interest>
   * :fieldAppliesToForm <personForm> ;
   * :fieldAppliesToProperty foaf:topic_interest ;
   * :widgetClass form:DBPediaLookup .
   *  <pre>
   */
  def updateFormForClass(formSyntax: AbstractForm): AbstractForm = {
    val updatedFields = for (field <- formSyntax.fields) yield {
      val fieldSpecs = formConfiguration.lookFieldSpecInConfiguration(field.property)
      fieldSpecs.map {
        fieldSpec =>
          val triples = find(graph, fieldSpec.subject, ANY, ANY)
          for (t <- triples)
            field.addTriple(t.subject, t.predicate, t.objectt)
      }
    }
    formSyntax
  }

  def addTypeTriple(subject: Rdf#Node, classs: Rdf#URI,
    fields: Iterable[Entry]): Seq[Entry] = {
    val formEntry = new ResourceEntry(
      // TODO not I18N:
      "type", "class",
      rdf.typ, ResourceValidator(Set(owl.Class)), classs,
      alreadyInDatabase = false)
    (fields ++ Seq(formEntry)).toSeq
    //    fields :+ formEntry
  }

  /** find fields from given Instance subject */
  private def fieldsFromSubject(subject: Rdf#Node, graph: Rdf#Graph): Seq[Rdf#URI] = {
    ops.getPredicates(graph, subject).toSet.toSeq
  }

  /**
   * try to get rdfs:label, comment, rdf:type,
   * or else display terminal Part of URI as label;
   *  taking in account multi-valued properties
   */
  private def makeEntries(subject: Rdf#Node, prop: Rdf#URI, ranges: Set[Rdf#URI]): Seq[Entry] = {
    Logger.getRootLogger().info(s"makeEntry subject $subject, prop $prop")
    val label = getHeadStringOrElse(prop, rdfs.label, terminalPart(prop))
    val comment = getHeadStringOrElse(prop, rdfs.comment, "")
    val propClasses = objectsQuery(prop, RDFPrefix[Rdf].typ)
    val objects = objectsQuery(subject, prop)
    val result = scala.collection.mutable.ArrayBuffer[Entry]()
    val rangeClasses = objectsQueries(ranges, RDFPrefix[Rdf].typ)

    for (obj <- objects) addOneEntry(obj)
    if (objects isEmpty) addOneEntry(nullURI)

    def makeBN(label: String, comment: String,
      property: ObjectProperty, validator: ResourceValidator,
      value: Rdf#BNode,
      typ: Rdf#URI = nullURI) = {
      new BlankNodeEntry(label, comment, prop, ResourceValidator(ranges), value,
        type_ = typ) {
        override def getId: String = ops.fromBNode(value.asInstanceOf[Rdf#BNode])
      }
    }

    def addOneEntry(object_ : Rdf#Node) = {

      def literalEntry = new LiteralEntry(label, comment, prop, DatatypeValidator(ranges),
        getStringOrElse(object_, "..empty.."),
        type_ = rdfh.nodeSeqToURISeq(ranges).headOption.getOrElse(nullURI))
      def resourceEntry = {
        ops.foldNode(object_)(
          object_ => new ResourceEntry(label, comment, prop, ResourceValidator(ranges), object_,
            alreadyInDatabase = true, valueLabel = instanceLabel(object_),
            type_ = rdfh.nodeSeqToURISeq(ranges).headOption.getOrElse(nullURI)),
          object_ => makeBN(label, comment, prop, ResourceValidator(ranges), object_, typ = rdfh.nodeSeqToURISeq(ranges).headOption.getOrElse(nullURI)
          ),
          object_ => new LiteralEntry(label, comment, prop, DatatypeValidator(ranges),
            getStringOrElse(object_, "..empty.."),
            type_ = rdfh.nodeSeqToURISeq(ranges).headOption.getOrElse(nullURI))
        )
      }
      val xsdPrefix = XSDPrefix[Rdf].prefixIri
      val rdf = RDFPrefix[Rdf]
      val rdfs = RDFSPrefix[Rdf]

      val entry = rangeClasses match {
        case _ if rangeClasses.exists { c => c.toString startsWith (xsdPrefix) } => literalEntry
        case _ if rangeClasses.contains(rdfs.Literal) => literalEntry
        case _ if propClasses.contains(owl.DatatypeProperty) => literalEntry
        case _ if propClasses.contains(owl.ObjectProperty) => resourceEntry
        case _ if rangeClasses.contains(owl.Class) => resourceEntry
        case _ if rangeClasses.contains(rdf.Property) => resourceEntry
        case _ if ranges.contains(owl.Thing) => resourceEntry
        //        case _ if ops.isURI(object_ ) => resourceEntry
        case _ if (isURIorBN(object_)) => resourceEntry
        case _ if object_.toString.startsWith("_:") => resourceEntry
        case _ => literalEntry
      }
      result += addPossibleValues(entry, ranges)
    }
    result
  }

  def isURIorBN(node: Rdf#Node) = ops.foldNode(node)(identity, identity, x => None) != None
  def isURI(node: Rdf#Node) = ops.foldNode(node)(identity, x => None, x => None) != None

  /**
   * compute terminal Part of URI, eg
   *  Person from http://xmlns.com/foaf/0.1/Person
   *  Project from http://usefulinc.com/ns/doap#Project
   *  NOTE: code related for getting the ontology prefix
   */
  def terminalPart(uri: Rdf#URI): String = {
    uriOps.getFragment(uri) match {
      case None => uriOps.lastSegment(uri)
      case Some(frag) => frag
    }
  }

  /**
   * get "first" String value (RDF object) Or Else given default;
   *  use application language
   */
  private def getHeadStringOrElse(subject: Rdf#URI, predicate: Rdf#URI, default: String): String = {
    //    println("getHeadStringOrElse: " + subject + " " + predicate) // debug
    objectsQuery(subject, predicate) match {
      case ll if ll == Set.empty => default
      case ll => getPreferedLanguageFromValues(ll)
    }
  }

  /** get Prefered Language value From Values */
  private def getPreferedLanguageFromValues(values: Iterable[Rdf#Node]): String = {
    def computeValues(): (String, String, String) = {
      var preferedLanguageValue = ""
      var enValue = ""
      var noLanguageValue = ""
      for (value <- values) {
        foldNode(value)(
          x => (), x => (),
          value => {
            val tt = fromLiteral(value)
            val (raw, uri, langOption) = fromLiteral(value)
            // println("getPreferedLanguageFromValues: " + (raw, uri, langOption) )
            langOption match {
              case Some(language) =>
                if (language == preferedLanguage) preferedLanguageValue = raw
                else if (language == "en")
                  enValue = raw
              case None => noLanguageValue = raw
              case _ =>
            }
          }
        )
      }
      println(s"preferedLanguageValue: $preferedLanguageValue , enValue $enValue, noLanguageValue $noLanguageValue")
      (preferedLanguageValue, enValue, noLanguageValue)
    }
    val (preferedLanguageValue, enValue, noLanguageValue) = computeValues
    (preferedLanguageValue, enValue, noLanguageValue) match {
      case _ if (preferedLanguageValue != "") => preferedLanguageValue
      case _ if (enValue != "") => enValue
      case _ if (noLanguageValue != "") => noLanguageValue
    }
  }

  /**
   * get first ?OBJ such that:
   *   subject predicate ?OBJ	,
   *   or returns default
   */
  private def getHeadOrElse(subject: Rdf#Node, predicate: Rdf#URI,
    default: Rdf#URI = nullURI): Rdf#URI = {
    objectsQuery(subject, predicate) match {
      case ll if ll.isEmpty => default
      case ll if (isURI(ll.head)) => ll.head.asInstanceOf[Rdf#URI]
      case _ => default
    }
  }

  private def getHeadValueOrElse(subjects: Set[Rdf#Node], predicate: Rdf#URI): Rdf#Node = {
    val values = objectsQueries(subjects, predicate)
    values.headOption match {
      case Some(x) => x
      case _ => nullURI
    }
  }

  def classFromSubject(subject: Rdf#Node) = {
    getHeadOrElse(subject, rdf.typ)
  }
}
