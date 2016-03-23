package deductions.runtime.services

import java.net.URLDecoder
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Try
import org.apache.log4j.Logger
import org.w3.banana.FOAFPrefix
import org.w3.banana.RDF
import org.w3.banana.TryW
import deductions.runtime.dataset.RDFStoreLocalProvider
import deductions.runtime.semlogs.LogAPI
import deductions.runtime.utils.Timer
import deductions.runtime.abstract_syntax.UnfilledFormFactory

trait FormSaver[Rdf <: RDF, DATASET]
    extends RDFStoreLocalProvider[Rdf, DATASET]
    with TypeAddition[Rdf, DATASET]
    with HttpParamsManager[Rdf]
    with LogAPI[Rdf]
    with Configuration
    with SaveListenersManager[Rdf]
    with Timer
    with URIManagement {

  import ops._
  import sparqlOps._
  import rdfStore.transactorSyntax._
  import rdfStore.graphStoreSyntax._

  val logger:Logger //  = Logger.getRootLogger()

  /** save triples in named graph given by HTTP parameter "graphURI";
   *  other HTTP parameters are original triples in Turtle
   * @param map a raw map of HTTP response parameters
   * @return main subject URI
   * transactional
   */
  def saveTriples(httpParamsMap: Map[String, Seq[String]])
      ( implicit userURI: String = "" ): Option[String]
      = {
    logger.debug(s"FormSaver.saveTriples httpParamsMap $httpParamsMap")
    println(s"""saveTriples: userURI <$userURI>""" )
    val uriArgs = httpParamsMap.getOrElse("uri", Seq())
    val subjectUriOption = uriArgs.find { uri => uri != "" }
    val graphURIOption = httpParamsMap.getOrElse("graphURI", Seq()).headOption
    println(s"FormSaver.saveTriples uri $subjectUriOption, graphURI $graphURIOption")

    val triplesToAdd = ArrayBuffer[Rdf#Triple]()
    val triplesToRemove = ArrayBuffer[Rdf#Triple]()

    subjectUriOption match {
      case Some(uri0) =>
        val subjectUri = URLDecoder.decode(uri0, "utf-8") // TODO uri0 ?
        // named graph in which to save:
        val graphURI =
          if (graphURIOption == Some("")) subjectUri
          else URLDecoder.decode(graphURIOption.getOrElse(uri0), "utf-8") // TODO no decode
        httpParamsMap.map {
          case (param0, objects) =>
            val param = URLDecoder.decode(param0, "utf-8") // TODO no decode
            logger.debug(s"saveTriples: httpParam decoded: $param")
            if (param != "url" &&
              param != "uri" &&
              param != "graphURI") {
              val try_ = Try {
                val comingBackTriple = httpParam2Triple(param)
                logger.debug(s"saveTriples: triple from httpParam: $comingBackTriple")
                computeDatabaseChanges(comingBackTriple, objects)
              }
              try_ match {
                case f: Failure[_] => logger.error("saveTriples: " + f)
                case _ =>
              }
            }
        }
        doSave(graphURI)
      case _ =>
    }


    def computeDatabaseChanges(originalTriple: Rdf#Triple, objectsFromUser: Seq[String]) {
      val foaf = FOAFPrefix[Rdf]
      if (originalTriple.predicate == foaf.firstName)
        println(foaf.firstName)
      objectsFromUser.map { objectStringFromUser =>
        // NOTE: a single element in objects
        val objectFromUser = foldNode(originalTriple.objectt)(
          _ => { if( objectStringFromUser.startsWith("_:") )
                BNode(objectStringFromUser.substring(2))
            else {
              if (objectStringFromUser != "")
                println(s"objectStringFromUser $objectStringFromUser changed: spaces removed")
                URI( // UnfilledFormFactory.
                    makeURIFromString(objectStringFromUser) )
            }
          },
          _ => BNode(objectStringFromUser.replaceAll(" ", "_")), // ?? really do this ?
          _ => Literal(objectStringFromUser))
        if (originalTriple.objectt != objectStringFromUser) {
          if (objectStringFromUser != "")
            triplesToAdd +=
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

    /** transactional */
    def doSave(graphURI: String)
    ( implicit userURI: String = graphURI ) {
      val transaction = dataset.rw({
        time("removeTriples",
          rdfStore.removeTriples( dataset,
            URI(graphURI),
            triplesToRemove.toIterable))
        val res =
          time("appendToGraph",
            rdfStore.appendToGraph( dataset,
              URI(graphURI),
              makeGraph(triplesToAdd)))

        /* TODO maybe in the hook here: return the future to print later that it has been done */
        callSaveListeners(triplesToAdd, triplesToRemove)
        
        Future {
          dataset.rw({
            addTypes(triplesToAdd.toSeq,
              Some(URI(graphURI)))
          })
        }
        res
      }) // .flatMap { identity }

      val f = transaction.asFuture

      f onSuccess {
        case _ =>
          logger.info(s""" Successfully stored ${triplesToAdd.size} triples
            ${triplesToAdd.mkString(", ")}
            and removed ${triplesToRemove.size}
            ${triplesToRemove.mkString(", ")}
          in graph $graphURI""")
      }
      f.onFailure { case t => println(s"doSave: Failure $t") }
    }

    return subjectUriOption
  }

}

