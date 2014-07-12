/* copyright the Déductions Project
under GNU Lesser General Public License
http://www.gnu.org/licenses/lgpl.html
$Id$
 */
package deductions.runtime.abstract_syntax

import scala.collection.mutable

import org.w3.banana.PointedGraph
import org.w3.banana.RDF
import org.w3.banana.RDFOps
import org.w3.banana.RDFPrefix
import org.w3.banana.RDFSPrefix
import org.w3.banana.XSDPrefix
import org.w3.banana.diesel.toPointedGraphW

//import deductions.Namespaces

class FormSyntaxFactory[Rdf <: RDF](graph: Rdf#Graph) 
( implicit ops: RDFOps[Rdf] )
extends FormModule[Rdf#URI] {

  val nullURI : Rdf#URI = ops.URI( "http://null.com#" ) // TODO better : "" ????????????

    /** For each given property (props)
   *  look at its rdfs:range ?D
   *  see if ?D is a datatype or an OWL or RDFS class */
  def createForm(subject: Rdf#URI,
    props: Seq[Rdf#URI]): FormSyntax[Rdf#URI] = {
    val fields = mutable.ArrayBuffer[Entry]()
    for (prop <- props) {
      val ranges = oQuery( prop, RDFSPrefix[Rdf].range )
      val rangesSize = ranges . size
      val mess = if( rangesSize > 1 ) {
        "WARNING: ranges " + ranges + " for property " + prop + " are multiple."
      } else if( rangesSize == 0 ) {
        "WARNING: There is no range for property " + prop
      } else ""
      println( mess, System.err)
      fields.append( makeEntry(subject, prop, ranges) )
    }
    FormSyntax(subject, fields)
  }

  private def makeEntry(subject: Rdf#URI, prop: Rdf#URI, ranges:Set[Rdf#Node]) : Entry = {
    val label =		getHeadStringOrElse( prop, RDFSPrefix[Rdf].label, prop.toString )
    val comment =	getHeadStringOrElse( prop, RDFSPrefix[Rdf].comment, "" )
    val propClass = getHeadOrElse( prop, RDFPrefix[Rdf].typ )
    val firstOobject = getHeadValueOrElse( Set(subject), prop ) // TODO associate each object with its property
    val classs = getHeadValueOrElse( ranges, RDFPrefix[Rdf].typ )
    def literalEntry = LiteralEntry(label, comment, prop, DatatypeValidator(propClass)
        , getStringOrElse(firstOobject, "" ) ) // TODO classs ?
    def resourceEntry = ResourceEntry(label, comment, prop, ResourceValidator(propClass)
        , firstOobject.asInstanceOf[Rdf#URI] ) // TODO bnode

//    val owl = OWLPrefix[Rdf]
    val owlPrefix = "http://www.w3.org/2002/07/owl#"

    /* see if range class is an XSD datatype,
     * or else if property is a DatatypeProperty
     * or else if property is an ObjectProperty
     * or else we arbitrarily set range String */
    val formEntry = classs match {
      case cl if cl.toString startsWith( XSDPrefix[Rdf] . prefixIri)  =>
        literalEntry
      case _ if ops.fromUri(propClass) ==
        // TODO use Banana
        owlPrefix + "DatatypeProperty" =>
        literalEntry
      case _ if ops.fromUri(propClass) ==
        owlPrefix + "ObjectProperty" =>
        resourceEntry
      case _ => literalEntry
    }
    formEntry
  }

  /** */
  private def getHeadStringOrElse(subject: Rdf#URI, predicate: Rdf#URI, default: String): String = {
    oQuery(subject.asInstanceOf[Rdf#URI], predicate) match {
      case ll if ll == Set.empty => default
      case ll =>
        val n = ll.head
        getStringOrElse(n, default)
    }
  }

  private def getStringOrElse(n: Rdf#Node, default: String): String = {
    ops.foldNode(n)(_ => default, _ => default, l =>
      l.toString
      // TODO ???????
//      ops.foldLiteral(l)(
//        tl => ops.fromTypedLiteral(tl)._1,
//        ll => ops.fromLangLiteral(ll)._1)
      )
  }

  private def getHeadOrElse(subject: Rdf#URI, predicate: Rdf#URI,
    default:Rdf#URI=nullURI ) : Rdf#URI= {
      oQuery( subject, predicate )
      match {
      case ll if ops.isURI[Unit](ll.head) => ll.head.asInstanceOf[Rdf#URI]
      case _ => default
      }
  }

  private def getHeadValueOrElse(subjects: Set[Rdf#Node], predicate: Rdf#URI ) : Rdf#Node = {
      val values = for( subject <- subjects;
          values <- oQuery( subject.asInstanceOf[Rdf#URI], predicate )
          ) yield values
      values.headOption match {
      case Some(x) => x
      case _ => nullURI
      }
    }
  
  /** Query for object in triple, given subject & predicate */
  private def oQuery(subject: Rdf#URI, predicate: Rdf#URI ): Set[Rdf#Node] = {
    val pg = PointedGraph[Rdf]( subject, graph )
    val objects = pg / predicate
    objects.map(_.pointer).toSet
  }

//  def oQuery2[T](subject: Rdf#URI, predicate: Rdf#URI, action: Rdf#Node => T ) : Set[T] = {
//    val pg = PointedGraph[Rdf]( subject, graph )
//    val objects = pg / predicate
//    val r = objects.map( obj => action(obj.pointer) )
//    r.toSet
//  }

  private def printGraph(answers: Rdf#Graph) {
    val iterable = ops.graphToIterable(answers)
    for (t <- iterable) {
      println(t)
      val (subj, pred, obj) = ops.fromTriple(t)
    }
  }
}