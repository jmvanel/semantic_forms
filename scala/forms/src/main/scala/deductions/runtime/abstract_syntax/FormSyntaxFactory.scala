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

/** Factory for an abstract Form Syntax;
 *   */
class FormSyntaxFactory[Rdf <: RDF]
(graph: Rdf#Graph)
( implicit ops: RDFOps[Rdf],
    uriOps: URIOps[Rdf],
    rdfDSL: RDFDSL[Rdf]
)
extends // RDFOpsModule with 
FormModule[Rdf#Node, Rdf#URI] {

  lazy val nullURI // : Rdf#URI 
    = ops.URI( "http://null.com#" ) // TODO better : "" ????????????
  val rdfs = RDFSPrefix[Rdf]
  
  /**create Form from an instance (subject) URI */
  def createForm(subject: Rdf#Node ) : FormSyntax[Rdf#Node, Rdf#URI] = {
     val props = fields(subject, graph)
     createForm(subject, props )
  }

  /** For each given property (props)
   *  look at its rdfs:range ?D
   *  see if ?D is a datatype or an OWL or RDFS class */
  def createForm(subject: Rdf#Node,
    props: Seq[Rdf#URI]): FormSyntax[Rdf#Node, Rdf#URI] = {
    Logger.getRootLogger().info(s"createForm subject $subject, props $props")
    val fields = mutable.ArrayBuffer[Entry]()
    for (prop <- props) {
      Logger.getRootLogger().info(s"createForm subject $subject, prop $prop")
      val ranges = extractURIs( oQuery( prop, rdfs.range ) )
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
//  private def fields(subject: Rdf#URI, graph: Rdf#Graph): Seq[Rdf#URI] = {
  private def fields(subject: Rdf#Node, graph: Rdf#Graph): Seq[Rdf#URI] = {
    rdfDSL.getPredicates(graph, subject).toSet.toSeq
  }

  /**
   * try to get rdfs:label, comment, rdf:type,
   * or else display terminal Part of URI as label;
   *  taking in account multi-valued properties
   */
//  private def makeEntry(subject: Rdf#URI, prop: Rdf#URI, ranges: Set[Rdf#URI]): Seq[Entry] = {
  private def makeEntry(subject: Rdf#Node, prop: Rdf#URI, ranges: Set[Rdf#URI]): Seq[Entry] = {
    Logger.getRootLogger().info(s"makeEntry subject $subject, prop $prop")
    val label = getHeadStringOrElse(prop, rdfs.label, terminalPart(prop))
    val comment = getHeadStringOrElse(prop, rdfs.comment, "")
//        if( prop.toString.contains("workInfoHomepage")) {
//          println
//        }
    val propClasses = oQuery(prop, RDFPrefix[Rdf].typ)
    val objects = oQuery(subject, prop)
    val result = scala.collection.mutable.ArrayBuffer[Entry]()
    val rangeClasses = oQuery(ranges, RDFPrefix[Rdf].typ)

    def makeBN(label: String, comment: String,
      property: ObjectProperty, validator: ResourceValidator,
      value: Rdf#BNode) = {
      new BlankNodeEntry(label, comment, prop, ResourceValidator(ranges), value) {
    	 override def getId : String = ops.fromBNode(value.asInstanceOf[Rdf#BNode])
      }
    }
    
    for (object_ <- objects) {
      def literalEntry = LiteralEntry(label, comment, prop, DatatypeValidator(ranges), getStringOrElse(object_.pointer, "<empty>"))
      def resourceEntry = {
        ops.foldNode(object_)(
            object_ => ResourceEntry(label, comment, prop, ResourceValidator(ranges), object_),
//                object_.pointer.asInstanceOf[Rdf#URI]),
            object_ => makeBN(label, comment, prop, ResourceValidator(ranges), object_),
              // BlankNodeEntry(label, comment, prop, ResourceValidator(ranges), object_),
            object_ => LiteralEntry(label, comment, prop, DatatypeValidator(ranges),
                getStringOrElse(object_.pointer, "<empty>"))
            )
//        ResourceEntry(label, comment, prop, ResourceValidator(ranges), object_.pointer.asInstanceOf[Rdf#URI])
      }
      val owl = OWLPrefix[Rdf]
      val xsdPrefix = XSDPrefix[Rdf].prefixIri
      val rdf  = RDFPrefix[Rdf]
      val rdfs = RDFSPrefix[Rdf]

      val entry = rangeClasses match {
        case _ if rangeClasses.exists{ c => c.toString startsWith (xsdPrefix)}
        => literalEntry
        case _ if rangeClasses.contains(rdfs.Literal) => literalEntry
        case _ if propClasses.contains(owl.DatatypeProperty) => literalEntry
        case _ if propClasses.contains(owl.ObjectProperty)	=> resourceEntry
        case _ if rangeClasses.contains(owl.Class) => resourceEntry
        case _ if rangeClasses.contains(rdf.Property) => resourceEntry
        //    case _ if ranges.contains(owl.Thing) => resourceEntry
        case _ if ranges.contains(ops.makeUri(owl.prefixIri + "Thing")) => resourceEntry
        case _ if ops.isURI(object_ ) => resourceEntry
        case _ if object_.toString.startsWith("_:") => resourceEntry
        case _ => literalEntry
      }
      result += entry
    }
    result
  }

  private def terminalPart(uri: Rdf#URI): String = {
    uriOps.getFragment(uri) match {
      case None => uriOps.lastSegment(uri)
      case Some(frag) => frag
    }
  }

  /** get "first" String value (RDF object) Or Else given default
   *  TODO use application language */
  private def getHeadStringOrElse(subject: Rdf#URI, predicate: Rdf#URI, default: String): String = {
    oQuery(subject, predicate) match {
      case ll if ll == Set.empty => default
      case ll =>
        val n = ll.head
        getStringOrElse(n, default)
    }
  }

  private def getStringOrElse(n: Rdf#Node, default: String): String = {
//    Logger.getRootLogger().info( "getStringOrElse ops " + ops + " Rdf#Node " + n + " " + n.getClass )
    ops.foldNode(n)(_ => default, _ => default, l => {
//      Logger.getRootLogger().info( "getStringOrElse 2 ops " + ops + " Rdf#Node " + l + " " + l.getClass )
//      Logger.getRootLogger().info( "getStringOrElse 2.1 "
//            + ops.asInstanceOf[JenaOps] . __xsdStringURI )
      val v = ops.fromLiteral(l)
//      Logger.getRootLogger().info( "getStringOrElse 3 " + v )
      v._1
    }
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
  def oQuery(subject: Rdf#Node, predicate: Rdf#URI ): Set[Rdf#Node] = {
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

  /*** from given Set of Rdf#Node , extract rdf#URI */
  def extractURIs(nodes:Set[Rdf#Node]) : Set[Rdf#URI] = {
    val v = nodes filter { node:Rdf#Node => ops.isURI(node) }
    v . map { node => node.asInstanceOf[Rdf#URI] }
  }

  def printGraph(graph: Rdf#Graph) {
    val iterable = ops.graphToIterable(graph)
    for (t <- iterable) {
      println(t)
      val (subj, pred, obj) = ops.fromTriple(t)
    }
  }
}