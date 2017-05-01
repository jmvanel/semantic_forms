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
import deductions.runtime.utils.RDFHelpers
import deductions.runtime.utils.URIManagement

trait FormSaver[Rdf <: RDF, DATASET]
    extends RDFStoreLocalProvider[Rdf, DATASET]
    with TypeAddition[Rdf, DATASET]
    with HttpParamsManager[Rdf]
    with LogAPI[Rdf]
    with SaveListenersManager[Rdf]
    with RDFHelpers[Rdf]
    with SPARQLHelpers[Rdf, DATASET]
    with Timer
    with URIManagement {

  import ops._

//  private lazy val foaf = FOAFPrefix[Rdf]

  /** save triples in named graph given by HTTP parameter "graphURI";
   *  other HTTP parameters are original triples in Turtle;
   *
   * Important HTTP parameters:
   * - uri: (main) subject URI
   * - graphURI: URI of named graph in which save triples will be saved
   * 
   * transactional
   * @param map a raw map of HTTP response parameters
   * @return main subject URI,
   *         type change flag */
  def saveTriples(httpParamsMap: Map[String, Seq[String]])
      ( implicit userURI: String = "" ): ( Option[String], Boolean)
      = {
    logger.debug(s"FormSaver.saveTriples httpParamsMap $httpParamsMap")
    logger.debug(s"""saveTriples: userURI <$userURI>""" )
    val uriArgs = httpParamsMap.getOrElse("uri", Seq())
    val encodedSubjectUriOption = uriArgs.find { uri => uri != "" }
    val graphURIOption = httpParamsMap.getOrElse("graphURI", Seq()).headOption
    logger.debug(s"FormSaver.saveTriples uri encoded $encodedSubjectUriOption, graphURI $graphURIOption")

    val triplesToAdd = ArrayBuffer[Rdf#Triple]()
    val triplesToRemove = ArrayBuffer[Rdf#Triple]()
    var typeChange = false

    // PENDING: require subject URI in parameter "uri" is probably not necessary (case of sub-forms and inverse triples)
    lazy val subjectUriOption = encodedSubjectUriOption match {
      case Some(subjectUri0) =>
        val subjectUri = URLDecoder.decode(subjectUri0, "utf-8")
        logger.debug(s"FormSaver.saveTriples subjectUri $subjectUri")

        // named graph in which to save:
        val graphURI =
          if (graphURIOption == Some("")) subjectUri
          else URLDecoder.decode(graphURIOption.getOrElse(subjectUri0), "utf-8") // TODO no decode
 
        httpParamsMap.map {
          case (param0, objects) =>
            val param = URLDecoder.decode(param0, "utf-8") // TODO no decode ??
            logger.debug(s"saveTriples: httpParam decoded: $param")
            if (param != "url" &&
              param != "uri" &&
              param != "graphURI") {
              val try_ = Try {
                val comingBackTriple = httpParam2Triple(param)
                logger.debug(s"saveTriples: triple from httpParam: {$comingBackTriple }")
                computeDatabaseChanges(comingBackTriple, objects)
              }
              try_ match {
                case f: Failure[_] => logger.error("saveTriples: " + f)
                case _ =>
              }
            }
        }
        doSave(graphURI)
        ( Some(subjectUri), typeChange)
      case _ => (None, typeChange)
    }

    //// end of saveTriples() body ////

    /* process a single triple from the form */
    def computeDatabaseChanges(originalTriple: Rdf#Triple, objectsFromUser: Seq[String]) {
      //      if (originalTriple.objectt == foaf.Document ) // predicate == foaf.firstName) logger.debug( "DDDDDDDDDDD "+ foaf.Document)
      logger.debug(s"computeDatabaseChanges: originalTriple: $originalTriple, objectsFromUser $objectsFromUser")
      objectsFromUser.map { objectStringFromUser =>
        // NOTE: a single element in objects
        val objectFromUser = foldNode(originalTriple.objectt)(
          _ => {
            if (objectStringFromUser.startsWith("_:"))
              BNode(objectStringFromUser.substring(2))
            else {
              if (objectStringFromUser != "")
                logger.debug(s"""computeDatabaseChanges: objectStringFromUser "$objectStringFromUser" changed: spaces removed""")
              URI( makeURIFromString(objectStringFromUser) )
            }
          },
          _ => BNode(objectStringFromUser.replaceAll(" ", "_")), // ?? really do this ?
          _ => Literal(objectStringFromUser))

        val originalData = nodeToString(originalTriple.objectt)
        val emptyUserInput: Boolean = objectStringFromUser == ""
        val differingUserInput: Boolean = objectStringFromUser != originalData
        val originalDataNonEmpty: Boolean = originalData != ""
        val newUserInput: Boolean = !emptyUserInput && differingUserInput

        logger.debug(s""">> originalData $originalData, emptyUserInput $emptyUserInput, differingUserInput $differingUserInput, originalDataNonEmpty $originalDataNonEmpty, newUserInput $newUserInput, objectStringFromUser $objectStringFromUser""")

        if( !emptyUserInput && differingUserInput ||
            // NOTE: the case of pre-filled rdfs:type
            originalTriple.predicate == rdf.typ)
          triplesToAdd +=
            makeTriple(originalTriple.subject, originalTriple.predicate, objectFromUser)

        if (originalDataNonEmpty && differingUserInput)
          triplesToRemove += originalTriple

        logger.debug("computeDatabaseChanges: predicate " + originalTriple.predicate + ", originalTriple.objectt: \"" +
          originalTriple.objectt.toString() + "\"" +
          ", objectStringFromUser \"" + objectStringFromUser + "\"")

        // FEATURE: change type triggers edit mode
        if (originalTriple.predicate == rdf.typ && differingUserInput) {
          println(s">>>> computeDatabaseChanges: typeChange! ($objectFromUser)")
          typeChange = true
        }
      }
    }

    /* transactional */
    def doSave(graphURI: String)
    ( implicit userURI: String = graphURI ) {
      val transaction = wrapInTransaction {
        time("removeTriples",
          rdfStore.removeTriples( dataset,
            URI(graphURI),
            triplesToRemove.toIterable), logger.isDebugEnabled() )
        val res =
          time("appendToGraph",
            rdfStore.appendToGraph( dataset,
              URI(graphURI),
              makeGraph(triplesToAdd)), logger.isDebugEnabled())
        logger.debug( s"doSave: triplesToAdd ${triplesToAdd.mkString(", ")}")
        res
      }

      Future { wrapInTransaction {
    	  addTypes(triplesToAdd.toSeq,
    			  Some(URI(graphURI)))
      } }

      /* TODO maybe: in the hook here: return the future to print later what has been done */
      callSaveListeners(triplesToAdd, triplesToRemove)

      val f = transaction.asFuture

      f onSuccess {
        case _ =>
          logger.info(s""" Successfully stored ${triplesToAdd.size} triples
            ${triplesToAdd.mkString(", ")}
            and removed ${triplesToRemove.size}
            triplesToRemove:
            ${triplesToRemove.mkString("\n")}
          in graph <$graphURI>""")
      }
      f.onFailure { case t => logger.error(s"doSave: Failure $t") }
    }

    return subjectUriOption
  }

}

