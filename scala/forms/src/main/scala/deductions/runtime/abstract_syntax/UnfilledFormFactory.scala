/**
 *
 */

package deductions.runtime.abstract_syntax

import org.w3.banana.RDF
import org.w3.banana.RDFOps
import org.w3.banana.URIOps
import org.w3.banana.RDFDSL
import org.w3.banana.OWLPrefix
import UnfilledFormFactory._
import org.w3.banana.RDFPrefix

/**
 * @author j.m. Vanel
 *
 */
object UnfilledFormFactory {
  var instanceURIPrefix = "http://assemblee-virtuelle.org/resource/"
  def makeId : String = {
    val r = instanceURIPrefix + System.currentTimeMillis() + "-" + System.nanoTime()
//    currentId = currentId + 1
    r
  }
}

class UnfilledFormFactory[Rdf <: RDF](graph: Rdf#Graph,
    preferedLanguage:String="en")
    (implicit ops: RDFOps[Rdf],
    		uriOps: URIOps[Rdf] )
  extends FormSyntaxFactory[Rdf](graph: Rdf#Graph, preferedLanguage) {
  
  /** create Form from a class URI */
  def createFormFromClass(classs: Rdf#URI): FormSyntax[Rdf#Node, Rdf#URI] = {
    if( lookFormConfiguration(classs).isEmpty ) { 
    	val props = fieldsFromClass(classs, graph)
    	createForm(ops.makeUri(makeId), props toSeq) // TODO , classs=classs)
    } else
        createForm(ops.makeUri(makeId), lookFormConfiguration(classs)toSeq) // TODO , classs=classs)
  }

  def lookFormConfiguration(classs: Rdf#URI): Seq[Rdf#URI] = {
    val rdf = RDFPrefix[Rdf]
    //    val forms = ops.getSubjects( graph, rdf.typ, form("TripleEdit") ) 
    val forms = ops.getSubjects(graph, form("classDomain"), classs)
    val form_ = forms.flatMap {
      form => ops.foldNode(form)(uri => Some(uri), bn => Some(bn), lit => None)
    }.headOption

    form_ match {
      case None => Seq()
      case Some(f) =>
        val props = ops.getObjects(graph, f, form("showProperties"))
        for( p <- props ) { println( "showProperties " + p) }
        val p = props.headOption
        nodeSeqToURISeq( rdfListToSeq(p) )
    }
  }

  /** recursively iterate on the Rdf#Node through rdf:first and rdf:rest */
  def rdfListToSeq(listOp: Option[Rdf#Node], result: Seq[Rdf#Node] = Seq()): Seq[Rdf#Node] = {
    listOp match {
      case None => result
      case Some(list) =>
        list match {
          case rdf.nil => result
          case _ =>
            val first = ops.getObjects(graph, list, rdf.first)
            val rest = ops.getObjects(graph, list, rdf.rest)
            result ++ first ++ rdfListToSeq(rest.headOption, result)
        }
    }
  }
  
  def nodeSeqToURISeq( s:Seq[Rdf#Node]) : Seq[Rdf#URI] = {
    s.collect{
      case uri if( ops.isURI(uri)) => ops.makeUri(uri.toString)
    }
  }
}