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

/** Factory for an Unfilled Form */
class UnfilledFormFactory[Rdf <: RDF](graph: Rdf#Graph,
    preferedLanguage:String="en")
    (implicit ops: RDFOps[Rdf],
    		uriOps: URIOps[Rdf] )
  extends FormSyntaxFactory[Rdf](graph: Rdf#Graph, preferedLanguage) {
  
	/** create Form from a class URI,
   *  looking up for Form Configuration within RDF graph in this class */
  def createFormFromClass(classs: Rdf#URI): FormSyntax[Rdf#Node, Rdf#URI] = {
  val formConfig = lookFormConfiguration(classs)
  if( formConfig.isEmpty ) { 
    val props = fieldsFromClass(classs, graph)
    createForm(ops.makeUri(makeId), props toSeq, classs)
  } else
      createForm(ops.makeUri(makeId), formConfig.toSeq, classs)
  }
  
  /** lookup for Form Configuration within RDF graph in this class */
  def lookFormConfiguration(classs: Rdf#URI): Seq[Rdf#URI] = {
    val rdf = RDFPrefix[Rdf]
    val forms = ops.getSubjects(graph, formPrefix("classDomain"), classs)
    val form_ = forms.flatMap {
      form => ops.foldNode(form)(uri => Some(uri), bn => Some(bn), lit => None)
    }.headOption
    println( "form_ " + form_)
    form_ match {
      case None => Seq()
      case Some(f) =>
//        val props = ops.getObjects(graph, f, form("showProperties"))
        val props = oQuery(f, formPrefix("showProperties") )
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
      case uri if( isURI(uri)) => ops.makeUri(uri.toString)
    }
  }
}