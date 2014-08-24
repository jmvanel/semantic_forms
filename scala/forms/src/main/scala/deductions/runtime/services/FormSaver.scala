package deductions.runtime.services

import org.w3.banana.SparqlEngine
import org.w3.banana.RDF
import org.w3.banana.RDFOps
import org.w3.banana.SparqlOps
import org.w3.banana.RDFStore
import scala.xml.Elem
import scala.concurrent._
import scala.concurrent.util._
import scala.concurrent.ExecutionContext.Implicits.global
import deductions.runtime.html.Form2HTML
import scala.concurrent.duration._
import org.apache.log4j.Logger
import org.w3.banana.RDFWriter
import org.w3.banana.Turtle
import java.io.ByteArrayOutputStream
import org.w3.banana.Command
import scala.collection.mutable.ArrayBuffer
import java.net.URLDecoder

class FormSaver[Rdf <: RDF](store: RDFStore[Rdf])(
  implicit ops: RDFOps[Rdf],
  sparqlOps: SparqlOps[Rdf],
  writer: RDFWriter[Rdf, Turtle]) {

  import ops._
  import sparqlOps._

  val sparqlEngine = SparqlEngine[Rdf](store)

  def saveTriples(map: Map[String, Seq[String]]) = {
    val uriOption = map.getOrElse("uri", Seq()).headOption
    uriOption match {
      case Some(uri) =>
        val v = map.map { case (prop, obj) => saveTriplesForProperty(uri, prop, obj) }
      case _ =>
    }
  }

  def saveTriplesForProperty(uri: String, prop: String, obj: Seq[String]) = {
    val triples = ArrayBuffer[Rdf#Triple]()
    obj.map(object_ => saveTriple(uri, prop, object_, triples))
    val init = store.execute {
      Command.append(makeUri(uri), triples)
    }

  }
  def saveTriple(uri: String, prop: String, obj0: String,
    triples: ArrayBuffer[Rdf#Triple]) = {
    val obj = URLDecoder.decode(obj0, "utf-8")
//    println("saveTriple: " + obj)
    triples +=
      makeTriple(
        makeUri(uri),
        makeUri(prop),
        // obj is literal, URI, or BN ?
        obj match {
          case obj if (obj startsWith ("LIT-")) =>
            val objActualValue = obj .substring(4)
            makeLiteral(objActualValue, xsd.string)
          case obj if (obj startsWith ("RES-")) =>
            val objActualValue = obj .substring(4)
            makeUri(objActualValue)
          case obj if (obj startsWith ("BLA-")) =>
            val objActualValue = obj .substring(4)
            makeBNodeLabel(objActualValue)
          case _ => makeLiteral("CASE NOT COVERED: " + obj, xsd.string)
        }
      )
  }

}

