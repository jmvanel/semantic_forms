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
import org.w3.banana.diesel.toPointedGraphW


/** Factory for an abstract Form Syntax */
class FormSyntaxFactory[Rdf <: RDF]
(graph: Rdf#Graph)
( implicit ops: RDFOps[Rdf],
    uriOps: URIOps[Rdf],
    rdfDSL: RDFDSL[Rdf]
)
extends // RDFOpsModule with 
FormModule[Rdf#URI] {

  val nullURI // : Rdf#URI 
    = ops.URI( "http://null.com#" ) // TODO better : "" ????????????

  def createForm(subject: Rdf#URI ) : FormSyntax[Rdf#URI] = {
     val props = fields(subject, graph)
     createForm(subject, props )
  }

  /** For each given property (props)
   *  look at its rdfs:range ?D
   *  see if ?D is a datatype or an OWL or RDFS class */
  def createForm(subject: Rdf#URI,
    props: Seq[Rdf#URI]): FormSyntax[Rdf#URI] = {
    val fields = mutable.ArrayBuffer[Entry]()
    for (prop <- props) {
      Logger.getRootLogger().info(s"createForm subject $subject, prop $prop")
      val ranges = extractRDFURIs( oQuery( prop, RDFSPrefix[Rdf].range ) )
      val rangesSize = ranges . size
      val mess = if( rangesSize > 1 ) {
        "WARNING: ranges " + ranges + " for property " + prop + " are multiple."
      } else if( rangesSize == 0 ) {
        "WARNING: There is no range for property " + prop
      } else ""
      println( mess, System.err)
      
      fields ++= ( makeEntry(subject, prop, ranges) )
    }
    FormSyntax(subject, fields)
  }

    /** find fields from given Instance subject */
  private def fields(subject: Rdf#URI, graph: Rdf#Graph): Seq[Rdf#URI] = {
    rdfDSL.getPredicates(graph, subject).toSeq
  }

  /**
   * try to get rdfs:label, comment, rdf:type,
   * or else display terminal Part of URI as label;
   *  taking in account multi-valued properties
   */
  private def makeEntry(subject: Rdf#URI, prop: Rdf#URI, ranges: Set[Rdf#URI]): Seq[Entry] = {
    Logger.getRootLogger().info(s"makeEntry subject $subject, prop $prop")
    val label = getHeadStringOrElse(prop, RDFSPrefix[Rdf].label, terminalPart(prop))
    val comment = getHeadStringOrElse(prop, RDFSPrefix[Rdf].comment, "")
        if( prop.toString.contains("schoolHomepage")) {
          println
        }
    val propClasses = oQuery(prop, RDFPrefix[Rdf].typ)
    val objects = oQuery(subject, prop)
    val result = scala.collection.mutable.ArrayBuffer[Entry]()
    val rangeClasses = oQuery(ranges, RDFPrefix[Rdf].typ)
    for (object_ <- objects) {
      def literalEntry = LiteralEntry(label, comment, prop, DatatypeValidator(ranges), getStringOrElse(object_.pointer, "<empty>"))
      def resourceEntry = ResourceEntry(label, comment, prop, ResourceValidator(ranges), object_.pointer.asInstanceOf[Rdf#URI])
      // TODO bnode

      val owl = OWLPrefix[Rdf]
      val xsdPrefix = XSDPrefix[Rdf].prefixIri
      val rdf = RDFPrefix[Rdf]

      val entry = rangeClasses match {
//        case _ if rangeClass.toString startsWith (xsdPrefix) => literalEntry
        case _ if rangeClasses.exists{ c => c.toString startsWith (xsdPrefix)}
        => literalEntry
        case _ if propClasses.contains(owl.DatatypeProperty) => literalEntry
        case _ if propClasses.contains(owl.ObjectProperty) => resourceEntry
        case _ if rangeClasses.contains(owl.Class) => resourceEntry
        case _ if rangeClasses.contains(rdf.Property) => resourceEntry
        //    case _ if ranges.contains(owl.Thing) => resourceEntry
        case _ if ranges.contains(ops.makeUri(owl.prefixIri + "Thing")) => resourceEntry
        case _ => literalEntry
      }
      result += entry
    }
    result
  }

  def terminalPart(uri: Rdf#URI): String = {
    uriOps.getFragment(uri) match {
      case None => uriOps.lastSegment(uri)
      case Some(frag) => frag
    }
  }

  /** get "first" String value (RDF object) Or Else given default */
  private def getHeadStringOrElse(subject: Rdf#URI, predicate: Rdf#URI, default: String): String = {
    oQuery(subject, predicate) match {
      case ll if ll == Set.empty => default
      case ll =>
        val n = ll.head
        getStringOrElse(n, default)
    }
  }

  private def getStringOrElse(n: Rdf#Node, default: String): String = {
    ops.foldNode(n)(_ => default, _ => default, l =>
      ops.fromLiteral(l)._1
      // TODO use application language
      )
  }

  private def getHeadOrElse(subject: Rdf#URI, predicate: Rdf#URI,
    default: Rdf#URI = nullURI): Rdf#URI = {
    oQuery(subject, predicate) match {
      case ll if ll.isEmpty => default
      case ll if ops.isURI[Unit](ll.head) => ll.head.asInstanceOf[Rdf#URI]
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
  private def oQuery(subject: Rdf#URI, predicate: Rdf#URI ): Set[Rdf#Node] = {
    val pg = PointedGraph[Rdf]( subject, graph )
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

  private def extractRDFURIs(nodes:Set[Rdf#Node]) : Set[Rdf#URI] = {
    val v = nodes filter { node:Rdf#Node => ops.isURI(node) }
    v . map { node => node.asInstanceOf[Rdf#URI] }
  }

//  def oQuery2[T](subject: Rdf#URI, predicate: Rdf#URI, action: Rdf#Node => T ) : Set[T] = {
//    val pg = PointedGraph[Rdf]( subject, graph )
//    val objects = pg / predicate
//    val r = objects.map( obj => action(obj.pointer) )
//    r.toSet
//  }

  def printGraph(answers: Rdf#Graph) {
    val iterable = ops.graphToIterable(answers)
    for (t <- iterable) {
      println(t)
      val (subj, pred, obj) = ops.fromTriple(t)
    }
  }
}