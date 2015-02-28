package deductions.runtime.services

import java.net.URLDecoder
import java.net.URLEncoder
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import org.w3.banana.RDF
import org.w3.banana.RDFOps
import org.w3.banana.RDFStore
import org.w3.banana.io.RDFWriter
import org.w3.banana.SparqlEngine
import org.w3.banana.SparqlOps
import org.w3.banana.io.Turtle
import deductions.runtime.jena.RDFStoreObject
import scala.util.Try
import scala.concurrent.Future
import org.w3.banana._
import org.w3.banana.jena.Jena

object FormSaverObject extends FormSaver[Jena]

class FormSaver[Rdf <: RDF]()(
    implicit ops: RDFOps[Rdf],
    sparqlOps: SparqlOps[Rdf],
    writer: RDFWriter[Rdf, Try, Turtle],
    rdfStore: RDFStore[Rdf, Try, RDFStoreObject.DATASET]) {

  import ops._
  import sparqlOps._
  import rdfStore.transactorSyntax._
  import rdfStore.graphStoreSyntax._
  val dataset = RDFStoreObject.dataset

  /**
   * @param map a raw map of HTTP response parameters
   *  TODO refactor : split 1) dedoding of the response 2) update of RDF database
   */
  def saveTriples(map: Map[String, Seq[String]]) = {
    println("FormSaver.saveTriples")
    val uriOption = map.getOrElse("uri", Seq()).headOption
    val graphURIOption = map.getOrElse("graphURI", Seq()).headOption
    println(s"FormSaver.saveTriples uri $uriOption, graphURI $graphURIOption")

    val triples = ArrayBuffer[Rdf#Triple]()
    val triplesToRemove = ArrayBuffer[Rdf#Triple]()

    uriOption match {
      case Some(uri0) =>
        //        val graphURI = URLDecoder.decode(uri0, "utf-8")
        val uri = URLDecoder.decode(uri0, "utf-8")
        val graphURI = URLDecoder.decode(graphURIOption.getOrElse(uri0), "utf-8")
        val v = map.map {
          case (prop0, obj) =>
            val prop = URLDecoder.decode(prop0, "utf-8")
            saveTriplesForProperty(uri, prop, obj, map)
        }
        doSave(graphURI: String)
      case _ =>
    }

    // ==== end of body of saveTriples ====

    def saveTriplesForProperty(uri: String, prop: String, objects: Seq[String], map: Map[String, Seq[String]]) = {
      objects.map(object_ => saveTriple(uri, prop, object_))
      println("triplesToRemove " + triplesToRemove)
      println("triples To add " + triples)
    }

    def processChange(uri: String, prop: String, obj: String): (Boolean, String) = {
      val originalValue = map.getOrElse(
        "ORIG-" + URLEncoder.encode(prop, "utf-8"), Seq("")).headOption.getOrElse("")
      val userValue = obj
      println("processChange " + uri + " " + prop + " " + obj +
        " userValue " + userValue + " originalValue " + originalValue)
      (userValue != originalValue, originalValue)
    }

    /** obj is literal, URI, or BN ? */
    def decodeHTTPParam(prop: String, obj: String): Rdf#Node = {
      prop match {
        case prop if (prop startsWith ("LIT-")) =>
          makeLiteral(obj, xsd.string)
        case prop if (prop startsWith ("RES-")) =>
          makeUri(obj)
        case prop if (prop startsWith ("BLA-")) =>
          makeBNodeLabel(obj)
        case _ => makeLiteral("FormSaver.decodeHTTPParam: CASE NOT COVERED: "
          + prop + " , "
          + obj, xsd.string)
      }
    }

    def saveTriple(uri: String, prop: String, obj0: String): Unit = {
      val obj = URLDecoder.decode(obj0, "utf-8")
      println("saveTriple: " + prop + " \"" + obj + "\"")
      if (prop != "url" &&
        prop != "uri" &&
        prop != "graphURI" &&
        !prop.startsWith("ORIG-")) {
        val (changed, originalValue) = processChange(uri, prop, obj)
        if (changed) {
          println("saveTriple: changed")
          triples +=
            makeTriple(
              makeUri(uri),
              makeUri(prop.substring(4)),
              decodeHTTPParam(prop, obj)
            )
          triplesToRemove +=
            makeTriple(
              makeUri(uri),
              makeUri(prop.substring(4)),
              decodeHTTPParam(prop, originalValue))
        }
      }
    }

    def doSave(graphURI: String) {
      import ops._
      val transaction =
        dataset.rw({
          dataset.removeTriples(makeUri(graphURI), triplesToRemove.toIterable)
          dataset.appendToGraph(makeUri(graphURI), makeGraph(triples))
        }).flatMap { identity }

      val f = transaction.asFuture

      f onSuccess {
        case _ =>
          println("Successfully stored triples in store")
          dataset.getGraph(makeUri(graphURI)).asFuture.
            onSuccess {
              case gr =>
                if (triplesToRemove.size > 0 ||
                  triples.size > 0) {
                  val graphAsString = writer.asString(gr, base = graphURI) getOrElse sys.error(
                    "coudn't serialize the graph")
                  println("Graph with modifications:\n" + graphAsString)
                }
            }
      }
    }
  }

}

