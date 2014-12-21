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

object FormSyntaxFactory {
  /** vocabulary for form specifications */
  val formVocabPrefix = "http://deductions-software.com/ontologies/forms.owl.ttl#"
}

/**
 * Factory for an abstract Form Syntax;
 */
class FormSyntaxFactory[Rdf <: RDF](graph: Rdf#Graph, preferedLanguage: String = "en")(implicit val ops: RDFOps[Rdf],
  val uriOps: URIOps[Rdf])
    extends // RDFOpsModule with 
    FormModule[Rdf#Node, Rdf#URI]
    with FieldsInference[Rdf] {
  import ops._

  lazy val nullURI = ops.URI("http://null.com#") // TODO better : "" ????????????
  val rdfs = RDFSPrefix[Rdf]

  val owl = OWLPrefix[Rdf]
  val owlThing = owl.prefixIri + "Thing"
  val rdf = RDFPrefix[Rdf]
  import FormSyntaxFactory._

  // TODO remove <<<<<<<<<<<<<<<<<<<<<<<<<<<
  private class PrefixBuilder2 // [Rdf <: RDF]
  (
    val prefixName: String,
    val prefixIri: String)
      //(implicit
      //  ops: RDFOps[Rdf]
      //)
      extends Prefix[Rdf] {
    import ops._
    override def toString: String = "Prefix(" + prefixName + ")"
    def apply(value: String): Rdf#URI = makeUri(prefixIri + value)
    def unapply(iri: Rdf#URI): Option[String] = {
      val uriString = fromUri(iri)
      if (uriString.startsWith(prefixIri))
        Some(uriString.substring(prefixIri.length))
      else
        None
    }
    def getLocalName(iri: Rdf#URI): Try[String] =
      unapply(iri) match {
        case None => Failure(LocalNameException(this.toString + " couldn't extract localname for " + iri.toString))
        case Some(localname) => Success(localname)
      }
  }

  val formPrefix: Prefix[Rdf] = new PrefixBuilder2 /*[Rdf]*/ ("form", formVocabPrefix)

  println("FormSyntaxFactory: preferedLanguage: " + preferedLanguage)

  /** create Form from an instance (subject) URI */
  def createForm(subject: Rdf#Node,
    editable: Boolean = false): FormSyntax[Rdf#Node, Rdf#URI] = {
    val props = fieldsFromSubject(subject, graph)
    val fromClass = if (editable) {
      val classs = classFromSubject(subject) // TODO several classes
      fieldsFromClass(classs, graph)
    } else Seq()
    createForm(subject, props ++ fromClass, nullURI)
  }

  /**
   * For each given property (props)
   *  look at its rdfs:range ?D
   *  see if ?D is a datatype or an OWL or RDFS class
   *  ( used for creating an empty Form from a class URI )
   */
  def createForm(subject: Rdf#Node,
    props: Seq[Rdf#URI], classs: Rdf#URI): FormSyntax[Rdf#Node, Rdf#URI] = {
    Logger.getRootLogger().info(s"createForm subject $subject, props $props")

    val f = for (prop <- props) yield {
      Logger.getRootLogger().info(s"createForm subject $subject, prop $prop")
      val ranges = extractURIs(oQuery(prop, rdfs.range))
      val rangesSize = ranges.size
      val mess = if (rangesSize > 1) {
        "WARNING: ranges " + ranges + " for property " + prop + " are multiple."
      } else if (rangesSize == 0) {
        "WARNING: There is no range for property " + prop
      } else ""
      println(mess, System.err)
      makeEntry(subject, prop, ranges)
    }
    val fields = f.flatMap { s => s }
    val fields2 = addTypeTriple(subject, classs, fields)
    FormSyntax(subject, fields2, classs)
  }

  def addTypeTriple(subject: Rdf#Node, classs: Rdf#URI,
    fields: Seq[Entry]): Seq[Entry] = {
    val formEntry = ResourceEntry(
      // TODO not I18N:
      "type", "class",
      rdf.typ, ResourceValidator(Set(owl.Class)), classs,
      alreadyInDatabase = false)
    fields :+ formEntry
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
  private def makeEntry(subject: Rdf#Node, prop: Rdf#URI, ranges: Set[Rdf#URI]): Seq[Entry] = {
    Logger.getRootLogger().info(s"makeEntry subject $subject, prop $prop")
    val label = getHeadStringOrElse(prop, rdfs.label, terminalPart(prop))
    val comment = getHeadStringOrElse(prop, rdfs.comment, "")
    val propClasses = oQuery(prop, RDFPrefix[Rdf].typ)
    val objects = oQuery(subject, prop)
    val result = scala.collection.mutable.ArrayBuffer[Entry]()
    val rangeClasses = oQuery(ranges, RDFPrefix[Rdf].typ)

    def makeBN(label: String, comment: String,
      property: ObjectProperty, validator: ResourceValidator,
      value: Rdf#BNode) = {
      new BlankNodeEntry(label, comment, prop, ResourceValidator(ranges), value) {
        override def getId: String = ops.fromBNode(value.asInstanceOf[Rdf#BNode])
      }
    }

    def addOneEntry(object_ : Rdf#Node) = {
      def literalEntry = LiteralEntry(label, comment, prop, DatatypeValidator(ranges),
        getStringOrElse(object_, "..empty.."))
      def resourceEntry = {
        ops.foldNode(object_)(
          object_ => ResourceEntry(label, comment, prop, ResourceValidator(ranges), object_,
            alreadyInDatabase = true),
          //                object_.pointer.asInstanceOf[Rdf#URI]),
          object_ => makeBN(label, comment, prop, ResourceValidator(ranges), object_),
          // BlankNodeEntry(label, comment, prop, ResourceValidator(ranges), object_),
          object_ => LiteralEntry(label, comment, prop, DatatypeValidator(ranges),
            getStringOrElse(object_, "..empty.."))
        )
        //        ResourceEntry(label, comment, prop, ResourceValidator(ranges), object_.pointer.asInstanceOf[Rdf#URI])
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
        //    case _ if ranges.contains(owl.Thing) => resourceEntry
        case _ if ranges.contains(ops.makeUri(owlThing)) => resourceEntry
        //        case _ if ops.isURI(object_ ) => resourceEntry
        case _ if (isURIorBN(object_)) => resourceEntry
        case _ if object_.toString.startsWith("_:") => resourceEntry
        case _ => literalEntry
      }
      result += entry
    }

    for (obj <- objects) addOneEntry(obj)

    // entry associated to prop
    if (objects isEmpty) {
      addOneEntry(nullURI)
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
    oQuery(subject, predicate) match {
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
        value match {
          case value: Rdf#Literal if (!isURIorBN(value)) =>
            val (raw, uri, langOption) = ops.fromLiteral(value)
            //            println("getPreferedLanguageFromValues: " +  (raw, uri, langOption) )
            langOption match {
              case Some(language) =>
                if (language == preferedLanguage) preferedLanguageValue = raw
                else if (language == "en")
                  //              case Some("en") => 
                  enValue = raw
              case None => noLanguageValue = raw
              case _ =>
            }
          case _ =>
        }
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

  private def getStringOrElse(n: Rdf#Node, default: String): String = {
    ops.foldNode(n)(_ => default, _ => default, l => {
      val v = ops.fromLiteral(l)
      v._1
    })
  }

  /**
   * get first ?OBJ such that:
   *   subject predicate ?OBJ	,
   *   or returns default
   */
  private def getHeadOrElse(subject: Rdf#Node, predicate: Rdf#URI,
    default: Rdf#URI = nullURI): Rdf#URI = {
    oQuery(subject, predicate) match {
      case ll if ll.isEmpty => default
      case ll if (isURI(ll.head)) => ll.head.asInstanceOf[Rdf#URI]
      case _ => default
    }
  }

  private def getHeadValueOrElse(subjects: Set[Rdf#Node], predicate: Rdf#URI): Rdf#Node = {
    val values = oQuery(subjects, predicate)
    values.headOption match {
      case Some(x) => x
      case _ => nullURI
    }
  }

  /** Query for objects in triple, given subject & predicate */
  def oQuery(subject: Rdf#Node, predicate: Rdf#URI): Set[Rdf#Node] = {
    val pg = PointedGraph[Rdf](subject, graph)
    val objects = pg / predicate
    objects.map(_.pointer).toSet
  }

  private def oQuery[T <: Rdf#Node](subjects: Set[T], predicate: Rdf#URI): Set[Rdf#Node] = {
    val values = for (
      subject <- subjects;
      values <- oQuery(subject.asInstanceOf[Rdf#URI], predicate)
    ) yield values
    values
  }

  /*** from given Set of Rdf#Node , extract rdf#URI */
  def extractURIs(nodes: Set[Rdf#Node]): Set[Rdf#URI] = {
    nodes.map {
      node =>
        ops.foldNode(node)(
          identity, identity, x => None
        )
    }
      .filter(_ != None)
      .map { node => node.asInstanceOf[Rdf#URI] }
  }

  def printGraph(graph: Rdf#Graph) {
    val iterable = ops.getTriples(graph)
    for (t <- iterable) {
      println(t)
      val (subj, pred, obj) = ops.fromTriple(t)
    }
  }

  def classFromSubject(subject: Rdf#Node) = {
    getHeadOrElse(subject, rdf.typ)
  }
}
