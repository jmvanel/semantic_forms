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
import deductions.runtime.dataset.RDFStoreLocalProvider
import org.w3.banana.jena.JenaModule
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import com.hp.hpl.jena.query.Dataset
import org.apache.log4j.Logger

object FormSaverObject extends FormSaver[Jena, Dataset] with JenaModule with RDFStoreLocalJena1Provider

trait FormSaver[Rdf <: RDF, DATASET]
    extends RDFStoreLocalProvider[Rdf, DATASET]
    with TurtleWriterModule
    with SparqlGraphModule
    with TypeAddition[Rdf, DATASET]
    with HttpParamsManager {

  import ops._
  import sparqlOps._
  import rdfStore.transactorSyntax._
  import rdfStore.graphStoreSyntax._

  val logger = Logger.getRootLogger()

  /**
   * @param map a raw map of HTTP response parameters
   * transactional
   */
  def saveTriples(httpParamsMap: Map[String, Seq[String]]) = {
    logger.debug(s"FormSaver.saveTriples httpParamsMap $httpParamsMap")
    val uriArgs = httpParamsMap.getOrElse("uri", Seq())
    val subjectUriOption = uriArgs.find { uri => uri != "" }
    val graphURIOption = httpParamsMap.getOrElse("graphURI", Seq()).headOption
    println(s"FormSaver.saveTriples uri $subjectUriOption, graphURI $graphURIOption")

    val triples = ArrayBuffer[Rdf#Triple]()
    val triplesToRemove = ArrayBuffer[Rdf#Triple]()

    subjectUriOption match {
      case Some(uri0) =>
        val subjectUri = URLDecoder.decode(uri0, "utf-8")
        val graphURI =
          if (graphURIOption == Some("")) subjectUri
          else URLDecoder.decode(graphURIOption.getOrElse(uri0), "utf-8")
        httpParamsMap.map {
          case (param0, objects) =>
            val param = URLDecoder.decode(param0, "utf-8")
            logger.debug(s"saveTriples: httpParam decoded: $param")
            if (param != "url" &&
              param != "uri" &&
              param != "graphURI") {
              Try {
                val triple = httpParam2Triple(param)
                logger.debug(s"saveTriples: triple from httpParam: $triple")
                computeDatabaseChanges(triple, objects)
              }
            }
        }
        doSave(graphURI)
      case _ =>
    }

    def computeDatabaseChanges(originalTriple: Rdf#Triple, objectsFromUser: Seq[String]) {
      objectsFromUser.map { objectStringFromUser =>
        // NOTE: a single element in objects
        val objectFromUser = foldNode(originalTriple.objectt)(
          _ => URI(objectStringFromUser),
          _ => BNode(objectStringFromUser), // ?? really do this ?
          _ => Literal(objectStringFromUser))
        if (originalTriple.objectt != objectStringFromUser) {
          if (objectStringFromUser != "")
            triples +=
              makeTriple(originalTriple.subject, originalTriple.predicate,
                objectFromUser)
          if (originalTriple.objectt.toString() != "")
            triplesToRemove += originalTriple
          logger.debug("computeDatabaseChanges: predicate " + originalTriple.predicate + ", originalTriple.objectt: \"" +
            originalTriple.objectt.toString() + "\"" +
            ", objectStringFromUser \"" + objectStringFromUser + "\"")
        }
      }
    }

    def time(mess: String, sourceCode: => Any) = {
      val start = System.currentTimeMillis()
      val res = sourceCode
      val end = System.currentTimeMillis()
      println(s"Time elapsed: $mess: ${end - start}")
      res
    }

    /** transactional */
    def doSave(graphURI: String) {
      val transaction = dataset.rw({
        time("removeTriples",
          dataset.removeTriples(makeUri(graphURI), triplesToRemove.toIterable))
        val res =
          time("appendToGraph",
            dataset.appendToGraph(makeUri(graphURI), makeGraph(triples)))

        /* TODO
         * add a hook here
         * return the future to print later that it has been done */
        Future { dataset.rw({ addTypes(triples, Some(URI(graphURI))) }) }
        res
      }) // .flatMap { identity }

      val f = transaction.asFuture

      f onSuccess {
        case _ =>
          logger.info(s""" Successfully stored ${triples.size} triples
            ${triples.mkString(", ")}
            and removed ${triplesToRemove.size}
            ${triplesToRemove.mkString(", ")}
          in graph $graphURI""")
          dataset.getGraph(makeUri(graphURI)).asFuture.
            onSuccess {
              case gr =>
                if (triplesToRemove.size > 0 ||
                  triples.size > 0) {
                  val writer: RDFWriter[Rdf, Try, Turtle] = turtleWriter
                  val graphAsString = writer.asString(gr, base = graphURI) getOrElse sys.error(
                    "coudn't serialize the graph")
                  println("Graph with modifications:\n" + graphAsString)
                }
            }
      }
      f.onFailure { case t => println(s"doSave: Failure $t") }
    }
  }

  //  private def saveTriples_old(httpParamsMap: Map[String, Seq[String]]) = {
  //    println(s"FormSaver.saveTriples $httpParamsMap")
  //    val uriArgs = httpParamsMap.getOrElse("uri", Seq())
  //    val subjectUriOption = uriArgs.find { uri => uri != "" }
  //    val graphURIOption = httpParamsMap.getOrElse("graphURI", Seq()).headOption
  //    println(s"FormSaver.saveTriples uri $subjectUriOption, graphURI $graphURIOption")
  //
  //    val triples = ArrayBuffer[Rdf#Triple]()
  //    val triplesToRemove = ArrayBuffer[Rdf#Triple]()
  //
  //    subjectUriOption match {
  //      case Some(uri0) =>
  //        val subjectUri = URLDecoder.decode(uri0, "utf-8")
  //        val graphURI =
  //          if (graphURIOption == Some("")) subjectUri
  //          else URLDecoder.decode(graphURIOption.getOrElse(uri0), "utf-8")
  //        httpParamsMap.map {
  //          case (param0, objects) =>
  //            val param = URLDecoder.decode(param0, "utf-8")
  //            val prop = httpParam2Property(param)
  //            saveTriplesForProperty(subjectUri, prop, objects, httpParamsMap)
  //        }
  //        //      println("triplesToRemove " + triplesToRemove)
  //        //      println("triples To add " + triples)
  //        doSave(graphURI)
  //      case _ =>
  //    }
  //
  //    // ==== end of body of saveTriples ====
  //
  //    def saveTriplesForProperty(uri: String, prop: String, objects: Seq[String],
  //      httpParamsMap: Map[String, Seq[String]]) = {
  //      objects.map(objectt => setTripleChanges(uri, prop, objectt))
  //    }
  //
  //    /*
  //     * process Change
  //     *  @return whether subject uri has changed, and the original Value
  //     */
  //    def processChange(uri: String, prop: String, obj: String): (Boolean, String) = {
  //      val originals = httpParamsMap.getOrElse(
  //        "ORIG-" + URLEncoder.encode(prop, "utf-8"), Seq(""))
  //      val originalValue = if (originals.size == 1)
  //        originals.headOption.getOrElse("")
  //      else ""
  //      val userValue = obj
  //      if (userValue != "" && originalValue != "")
  //        println(s"""processChange $uri $prop
  //        userValue "$userValue" originalValue "$originalValue" """)
  //      val changed = userValue != originalValue
  //      (changed, originalValue)
  //    }
  //
  //    /** obj is literal, URI, or BN ? */
  //    def decodeHTTPParam(propParam: String, obj: String): Rdf#Node = {
  //      propParam match {
  //        case prop if (prop startsWith ("LIT-")) =>
  //          makeLiteral(obj, xsd.string)
  //        case prop if (prop startsWith ("RES-")) =>
  //          makeUri(obj)
  //        case prop if (prop startsWith ("BLA-")) =>
  //          makeBNodeLabel(obj)
  //        case _ => makeLiteral("FormSaver.decodeHTTPParam: CASE NOT COVERED: "
  //          + propParam + " , "
  //          + obj, xsd.string)
  //      }
  //    }
  //
  //    /** populate lists of triples to add and remove */
  //    def setTripleChanges(uri: String, prop: String, obj0: String): Unit = {
  //      val objectt = URLDecoder.decode(obj0, "utf-8").trim()
  //      if (objectt != "") println("save Triple? prop " + prop + ", obj \"" + objectt + "\"")
  //      if (prop != "url" &&
  //        prop != "uri" &&
  //        prop != "graphURI" &&
  //        !prop.startsWith("ORIG-")) {
  //        val (changed, originalValue) = processChange(uri, prop, objectt)
  //        if (changed && objectt != "") {
  //          println(s"""save Triple: $prop changed from $originalValue to "$objectt" """)
  //          triples +=
  //            makeTriple(
  //              makeUri(uri),
  //              makeUri(prop.substring(4)),
  //              decodeHTTPParam(prop, objectt))
  //          if (originalValue != "")
  //            triplesToRemove +=
  //              makeTriple(
  //                makeUri(uri),
  //                makeUri(prop.substring(4)),
  //                decodeHTTPParam(prop, originalValue))
  //        }
  //      }
  //    }
  //
  //    /** transactional */
  //    def doSave(graphURI: String) {
  //      val transaction = dataset.rw({
  //        // dataset.removeTriples(makeUri(graphURI), triplesToRemove.toIterable)
  //        val res = dataset.appendToGraph(makeUri(graphURI), makeGraph(triples))
  //
  //        /* 
  //         * return the future to print later that it has been done */
  //        Future { dataset.rw({ addTypes(triples, Some(URI(graphURI))) }) }
  //        res
  //      }).flatMap { identity }
  //
  //      val f = transaction.asFuture
  //
  //      f onSuccess {
  //        case _ =>
  //          println(s""" Successfully stored ${triples.size} triples
  //            ${triples.mkString(", ")}
  //            and removed ${triplesToRemove.size}
  //            ${triplesToRemove.mkString(", ")}
  //          in graph $graphURI""")
  //          dataset.getGraph(makeUri(graphURI)).asFuture.
  //            onSuccess {
  //              case gr =>
  //                if (triplesToRemove.size > 0 ||
  //                  triples.size > 0) {
  //                  val writer: RDFWriter[Rdf, Try, Turtle] = turtleWriter
  //                  val graphAsString = writer.asString(gr, base = graphURI) getOrElse sys.error(
  //                    "coudn't serialize the graph")
  //                  println("Graph with modifications:\n" + graphAsString)
  //                }
  //            }
  //      }
  //      f.onFailure { case t => println(s"doSave: Failure $t") }
  //    }
  //  }

}

